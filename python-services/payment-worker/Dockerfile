FROM python:3.11-slim
WORKDIR /app

COPY payment-worker/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY shared ./shared
COPY payment-worker/ .

CMD ["python", "automation.py"]
