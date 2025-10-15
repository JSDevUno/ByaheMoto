from typing import Optional

from httpx import AsyncClient, Response

from ...settings import settings


class PayPal:
    __instance = None

    def __init__(self):
        self.client = AsyncClient()
        self.base_url = 'https://api-m.sandbox.paypal.com'
        self.client_id = settings.paypal_client_id
        self.client_secret = settings.paypal_client_secret
        self.access_token = None

    async def get_auth_token(self) -> Optional[str]:
        url = f'{self.base_url}/v1/oauth2/token'
        data = {'grant_type': 'client_credentials'}

        res: Response = await self.client.post(url, data=data, auth=(self.client_id, self.client_secret))

        if not res.is_success:
            return None

        self.access_token = res.json()['access_token']

        return self.access_token

    async def create_order(self, amount: float, currency: str = 'PHP'):
        url = f'{self.base_url}/v2/checkout/orders'
        headers = {'Authorization': f'Bearer {self.access_token}'}
        data = {
            'intent': 'CAPTURE',
            'purchase_units': [{
                'items': [{
                    'name': 'Top Up',
                    'unit_amount': {
                        'currency_code': currency,
                        'value': f'{amount:.2f}',
                    },
                    'quantity': '1'
                }],
                'amount': {
                    'currency_code': currency,
                    'value': f'{amount:.2f}',
                    'breakdown': {
                        'item_total': {
                            'currency_code': currency,
                            'value': f'{amount:.2f}',
                        },
                    }
                },
            }],
            'application_context': {
                'return_url': 'https://example.com/return',
                'cancel_url': 'https://example.com/cancel',
            }
        }

        return await self.client.post(url, headers=headers, json=data)

    async def get_order(self, order_id: str):
        url = f'{self.base_url}/v2/checkout/orders/{order_id}'
        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {self.access_token}',
        }

        return await self.client.get(url, headers=headers)

    async def capture_order(self, order_id: str):
        url = f'{self.base_url}/v2/checkout/orders/{order_id}/capture'
        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {self.access_token}',
            'PayPal-Request-Id': order_id,
            'Prefer': 'return=representation',
        }

        return await self.client.post(url, headers=headers, data={})

    def __new__(cls, *args, **kwargs):
        if cls.__instance is None:
            cls.__instance = super().__new__(cls, *args, **kwargs)

        return cls.__instance

    @classmethod
    async def create(cls, *args, **kwargs):
        if cls.__instance is None:
            cls.__instance = cls()

        await cls.__instance.get_auth_token()

        return cls.__instance
