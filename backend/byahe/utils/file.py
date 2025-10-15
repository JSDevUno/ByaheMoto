import pathlib
from datetime import datetime
from typing import Optional

from fastapi import UploadFile


def write_file(file: UploadFile, file_name: str, directory: Optional[str] = None) -> pathlib.Path:
    """
    Write file to disk
    :param file: UploadFile
    :param file_name: str
    :param directory: str
    :return: pathlib.Path
    """
    file_extension = pathlib.Path(file.filename).suffix # type: ignore
    uploads_dir = pathlib.Path('uploads') if directory is None else pathlib.Path(f'uploads/{directory}')
    uploads_dir.mkdir(parents=True, exist_ok=True)

    file_name = f'{file_name}-{int(datetime.now().timestamp())}{file_extension}'
    file_path = uploads_dir / file_name

    with file_path.open('wb') as f:
        f.write(file.file.read())

    return file_path
