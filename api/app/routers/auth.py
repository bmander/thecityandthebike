from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import or_
from sqlalchemy.orm import Session

from ..config import settings
from ..database import get_db
from ..dependencies import get_password_hash, verify_password, create_access_token, create_refresh_token, rotate_refresh_token
from ..models import User, RefreshToken
from ..rate_limit import AccountLockout, get_account_lockout, limiter
from ..schemas import UserRegister, UserLogin, Token, RefreshRequest, MessageResponse

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
@limiter.limit(lambda: settings.REGISTER_RATE_LIMIT)
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
@limiter.limit(lambda: settings.LOGIN_RATE_LIMIT)
def login(
    request: Request,
    data: UserLogin,
    db: Annotated[Session, Depends(get_db)],
    lockout: Annotated[AccountLockout, Depends(get_account_lockout)],
):
    client_ip = request.client.host if request.client else "unknown"

    if lockout.is_locked(data.username, client_ip, db):
        raise HTTPException(
            status_code=status.HTTP_423_LOCKED,
            detail={"msg": "Account temporarily locked due to too many failed attempts"},
        )

    user = db.query(User).filter(User.username == data.username).first()
    if not user or not verify_password(data.password, user.password_hash):
        lockout.record_failure(data.username, client_ip, db)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"msg": "Bad username or password"},
        )

    lockout.clear(data.username, db)
    access_token = create_access_token(subject=str(user.user_id))
    refresh_token = create_refresh_token(user_id=user.user_id, db=db)
    return {"access_token": access_token, "refresh_token": refresh_token, "token_type": "bearer"}


@router.post("/refresh", response_model=Token)
def refresh(data: RefreshRequest, db: Annotated[Session, Depends(get_db)]):
    access_token, new_refresh_token = rotate_refresh_token(data.refresh_token, db)
    return {"access_token": access_token, "refresh_token": new_refresh_token, "token_type": "bearer"}


@router.post("/logout", response_model=MessageResponse)
def logout(data: RefreshRequest, db: Annotated[Session, Depends(get_db)]):
    record = db.query(RefreshToken).filter(RefreshToken.token == data.refresh_token).first()
    if record:
        db.delete(record)
        db.commit()
    return {"msg": "Logged out"}
