from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import or_
from sqlalchemy.orm import Session

from ..config import settings
from ..database import get_db
from ..dependencies import get_password_hash, verify_password, create_access_token
from ..models import User
from ..rate_limit import AccountLockout, get_account_lockout, limiter
from ..schemas import UserRegister, UserLogin, Token, MessageResponse

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
@limiter.limit(settings.REGISTER_RATE_LIMIT)
def register(request: Request, data: UserRegister, db: Annotated[Session, Depends(get_db)]):
    existing = db.query(User).filter(
        or_(User.username == data.username, User.email == data.email)
    ).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={"msg": "User with that username or email already exists"},
        )

    password_hash = get_password_hash(data.password)
    user = User(username=data.username, email=data.email, password_hash=password_hash)
    db.add(user)
    db.commit()
    return {"msg": "User created"}


@router.post("/login", response_model=Token)
@limiter.limit(settings.LOGIN_RATE_LIMIT)
def login(
    request: Request,
    data: UserLogin,
    db: Annotated[Session, Depends(get_db)],
    lockout: Annotated[AccountLockout, Depends(get_account_lockout)],
):
    if lockout.is_locked(data.username):
        raise HTTPException(
            status_code=status.HTTP_423_LOCKED,
            detail={"msg": "Account temporarily locked due to too many failed attempts"},
        )

    user = db.query(User).filter(User.username == data.username).first()
    if not user or not verify_password(data.password, user.password_hash):
        lockout.record_failure(data.username)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"msg": "Bad username or password"},
        )

    lockout.clear(data.username)
    access_token = create_access_token(subject=str(user.user_id))
    return {"access_token": access_token, "token_type": "bearer"}
