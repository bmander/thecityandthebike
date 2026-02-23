import uuid as uuid_mod

from sqladmin import Admin, ModelView
from sqladmin.authentication import AuthenticationBackend
from sqlalchemy.orm import sessionmaker
from starlette.requests import Request

from .dependencies import verify_password
from .models import User, Bike, FenderSubmission, RefreshToken, LoginAttempt, Tag, ScoringEvent, DeviceBan


class AdminAuth(AuthenticationBackend):
    def __init__(self, secret_key: str, session_factory: sessionmaker):
        super().__init__(secret_key=secret_key)
        self.session_factory = session_factory

    async def login(self, request: Request) -> bool:
        form = await request.form()
        username = form.get("username")
        password = form.get("password")

        session = self.session_factory()
        try:
            user = session.query(User).filter(User.username == username).first()
            if not user or not verify_password(password, user.password_hash):
                return False
            if not user.is_admin:
                return False
            request.session.update({"user_id": str(user.user_id)})
            return True
        finally:
            session.close()

    async def logout(self, request: Request) -> bool:
        request.session.clear()
        return True

    async def authenticate(self, request: Request) -> bool:
        user_id = request.session.get("user_id")
        if not user_id:
            return False

        session = self.session_factory()
        try:
            try:
                uid = uuid_mod.UUID(user_id)
            except ValueError:
                return False
            user = session.query(User).filter(User.user_id == uid).first()
            if not user or not user.is_admin:
                return False
            return True
        finally:
            session.close()


class UserAdmin(ModelView, model=User):
    column_list = [User.user_id, User.username, User.email, User.is_admin, User.is_banned, User.android_id, User.created_at]
    column_searchable_list = [User.username, User.email]
    column_sortable_list = [User.username, User.email, User.created_at]
    form_excluded_columns = [User.password_hash, User.submissions]


class DeviceBanAdmin(ModelView, model=DeviceBan):
    column_list = [DeviceBan.id, DeviceBan.android_id, DeviceBan.banned_by, DeviceBan.reason, DeviceBan.created_at]
    column_searchable_list = [DeviceBan.android_id]
    column_sortable_list = [DeviceBan.created_at]


class BikeAdmin(ModelView, model=Bike):
    column_list = [Bike.id, Bike.bike_qr_id, Bike.provider, Bike.bike_brand, Bike.first_seen_at, Bike.last_seen_at]
    column_searchable_list = [Bike.bike_qr_id, Bike.bike_brand]
    column_sortable_list = [Bike.id, Bike.bike_qr_id, Bike.bike_brand, Bike.first_seen_at]
    form_excluded_columns = [Bike.submissions]


class FenderSubmissionAdmin(ModelView, model=FenderSubmission):
    column_list = [
        FenderSubmission.submission_id,
        FenderSubmission.user_id,
        FenderSubmission.bike_id,
        FenderSubmission.captured_date,
        FenderSubmission.uploaded_at,
    ]
    column_searchable_list = [FenderSubmission.bike_id]
    column_sortable_list = [FenderSubmission.captured_date, FenderSubmission.uploaded_at]


class RefreshTokenAdmin(ModelView, model=RefreshToken):
    column_list = [RefreshToken.id, RefreshToken.user_id, RefreshToken.expires_at, RefreshToken.created_at]
    column_sortable_list = [RefreshToken.created_at, RefreshToken.expires_at]


class LoginAttemptAdmin(ModelView, model=LoginAttempt):
    column_list = [LoginAttempt.id, LoginAttempt.username, LoginAttempt.attempted_at]
    column_searchable_list = [LoginAttempt.username]
    column_sortable_list = [LoginAttempt.attempted_at]


class TagAdmin(ModelView, model=Tag):
    column_list = [Tag.tag_id, Tag.submission_id, Tag.user_id, Tag.created_at]
    column_searchable_list = [Tag.submission_id]


class ScoringEventAdmin(ModelView, model=ScoringEvent):
    column_list = [
        ScoringEvent.event_id,
        ScoringEvent.user_id,
        ScoringEvent.event_type,
        ScoringEvent.points,
        ScoringEvent.submission_id,
        ScoringEvent.tag_id,
        ScoringEvent.created_at,
        ScoringEvent.revoked_at,
    ]
    column_searchable_list = [ScoringEvent.event_type]
    column_sortable_list = [ScoringEvent.created_at, ScoringEvent.points]


def setup_admin(app, engine, secret_key):
    session_factory = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    auth_backend = AdminAuth(secret_key=secret_key, session_factory=session_factory)
    admin = Admin(app=app, engine=engine, authentication_backend=auth_backend)
    admin.add_view(UserAdmin)
    admin.add_view(DeviceBanAdmin)
    admin.add_view(BikeAdmin)
    admin.add_view(FenderSubmissionAdmin)
    admin.add_view(RefreshTokenAdmin)
    admin.add_view(LoginAttemptAdmin)
    admin.add_view(TagAdmin)
    admin.add_view(ScoringEventAdmin)
    return admin
