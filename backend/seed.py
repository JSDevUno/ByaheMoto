from byahe.utils import database, hashing


async def seed():
    await database.connect()

    await database.user.create({
        'email': 'admin@byahe.com',
        'username': 'admin',
        'password': hashing.Hashing.hash('admin'),
        'full_name': 'Admin',
        'role': 'ADMIN',
        'wallet': {
            'create': {}
        },
        'identity': {
            'create': {
                'url': 'https://www.example.com',
                'file_path': '',
                'type': 'NONE'
            }
        }
    }, include={'wallet': True})

    await database.user.create({
        'email': 'driver@byahe.com',
        'username': 'driver',
        'password': hashing.Hashing.hash('driver'),
        'full_name': 'Driver',
        'role': 'DRIVER',
        'wallet': {
            'create': {}
        },
        'identity': {
            'create': {
                'url': 'https://www.example.com',
                'file_path': '',
                'type': 'DRIVER',
            }
        }
    })

    await database.user.create({
        'email': 'user@byahe.com',
        'username': 'user',
        'password': hashing.Hashing.hash('user'),
        'full_name': 'User',
        'role': 'USER',
        'wallet': {
            'create': {}
        },
        'identity': {
            'create': {
                'url': 'https://www.example.com',
                'file_path': '',
                'type': 'NONE',
            }
        }
    })

    await database.disconnect()


if __name__ == '__main__':
    import asyncio

    asyncio.run(seed())
