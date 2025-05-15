from flask import Flask, request, jsonify
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime
import uuid

from models import db, User, Bike, FenderSubmission
import config

def create_app():
    app = Flask(__name__)
    app.config.from_object(config.Config)
    db.init_app(app)
    JWTManager(app)

    @app.before_first_request
    def create_tables():
        db.create_all()

    @app.route('/auth/register', methods=['POST'])
    def register():
        data = request.get_json()
        if not data or not data.get('username') or not data.get('email') or not data.get('password'):
            return jsonify({'msg': 'Missing username, email, or password'}), 400
        username = data['username']
        email = data['email']
        password = data['password']
        if User.query.filter((User.username == username) | (User.email == email)).first():
            return jsonify({'msg': 'User with that username or email already exists'}), 409
        password_hash = generate_password_hash(password)
        user = User(username=username, email=email, password_hash=password_hash)
        db.session.add(user)
        db.session.commit()
        return jsonify({'msg': 'User created'}), 201

    @app.route('/auth/login', methods=['POST'])
    def login():
        data = request.get_json()
        if not data or not data.get('username') or not data.get('password'):
            return jsonify({'msg': 'Missing username or password'}), 400
        username = data['username']
        password = data['password']
        user = User.query.filter_by(username=username).first()
        if not user or not check_password_hash(user.password_hash, password):
            return jsonify({'msg': 'Bad username or password'}), 401
        access_token = create_access_token(identity=user.user_id)
        return jsonify({'access_token': access_token}), 200

    @app.route('/users/me', methods=['GET'])
    @jwt_required()
    def get_profile():
        user_id = get_jwt_identity()
        user = User.query.get(user_id)
        if not user:
            return jsonify({'msg': 'User not found'}), 404
        return jsonify(user.to_dict()), 200

    @app.route('/users/me/submissions', methods=['GET'])
    @jwt_required()
    def get_my_submissions():
        user_id = get_jwt_identity()
        submissions = FenderSubmission.query.filter_by(user_id=user_id).order_by(FenderSubmission.uploaded_at.desc()).all()
        return jsonify([s.to_dict() for s in submissions]), 200

    @app.route('/submissions', methods=['GET'])
    @jwt_required()
    def get_global_submissions():
        submissions = FenderSubmission.query.order_by(FenderSubmission.uploaded_at.desc()).all()
        return jsonify([s.to_dict() for s in submissions]), 200

    @app.route('/bikes/<string:bike_qr_id>/submissions', methods=['GET'])
    @jwt_required()
    def get_bike_submissions(bike_qr_id):
        bike = Bike.query.get(bike_qr_id)
        if not bike:
            return jsonify({'msg': 'Bike not found'}), 404
        submissions = FenderSubmission.query.filter_by(bike_qr_id=bike_qr_id).order_by(FenderSubmission.uploaded_at.desc()).all()
        return jsonify([s.to_dict() for s in submissions]), 200

    @app.route('/submissions', methods=['POST'])
    @jwt_required()
    def create_submission():
        user_id = get_jwt_identity()
        data = request.get_json()
        required_fields = ['bike_qr_id', 'image_url_original', 'image_url_processed', 'captured_at']
        if not data or not all(field in data for field in required_fields):
            return jsonify({'msg': 'Missing required submission fields'}), 400
        bike_qr_id = data['bike_qr_id']
        image_url_original = data['image_url_original']
        image_url_processed = data['image_url_processed']
        captured_at_str = data['captured_at']
        try:
            captured_at = datetime.fromisoformat(captured_at_str)
        except Exception:
            return jsonify({'msg': 'Invalid captured_at format, use ISO format'}), 400
        latitude = data.get('latitude')
        longitude = data.get('longitude')
        user_caption = data.get('user_caption')
        bike = Bike.query.get(bike_qr_id)
        now = datetime.utcnow()
        if bike:
            bike.last_seen_at = now
        else:
            bike = Bike(bike_qr_id=bike_qr_id, first_seen_at=now, last_seen_at=now)
            db.session.add(bike)
        submission = FenderSubmission(
            user_id=user_id,
            bike_qr_id=bike_qr_id,
            image_url_original=image_url_original,
            image_url_processed=image_url_processed,
            latitude=latitude,
            longitude=longitude,
            captured_at=captured_at,
            user_caption=user_caption
        )
        db.session.add(submission)
        db.session.commit()
        return jsonify(submission.to_dict()), 201

    return app

if __name__ == '__main__':
    app = create_app()
    app.run(host='0.0.0.0', port=5000, debug=True)