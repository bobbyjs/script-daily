"""
https://fastapi.tiangolo.com/zh/tutorial/security/oauth2-jwt/
"""

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer


class ReqContext:
    user_id: str

    def __init__(self, user_id: str):
        self.user_id = user_id


# Authorization: Bearer token
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")


async def get_current_user(token: str = Depends(oauth2_scheme)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if "d3a3bab9-57ae-4cc3-961c-f4ea5f23398b" != token:
        raise credentials_exception
    return "default"
