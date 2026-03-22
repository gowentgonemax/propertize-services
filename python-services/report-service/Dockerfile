FROM python:3.11-slim
WORKDIR /app

COPY report-service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY shared ./shared
COPY report-service/ .

EXPOSE 8090
CMD ["python", "main.py"]
