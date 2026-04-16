"""
Screening Pipeline Worker (OOP refactored)
Thin Kafka consumer loop; delegates scoring to ScreeningService.
"""
import sys, os, json, logging, time
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from shared.config import KAFKA_BOOTSTRAP
from services.screening_service import ScreeningService

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

service = ScreeningService()


def run_consumer():
    try:
        from kafka import KafkaConsumer, KafkaProducer
    except ImportError:
        log.error("kafka-python not installed. Run: pip install kafka-python")
        sys.exit(1)

    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )
    consumer = KafkaConsumer(
        "screening.initiated",
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id="screening-worker",
        auto_offset_reset="earliest",
        enable_auto_commit=False,
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    )

    log.info(f"Screening worker listening on 'screening.initiated' @ {KAFKA_BOOTSTRAP}")

    for message in consumer:
        try:
            event = message.value
            app_id = event.get("application_id")
            if not app_id:
                log.warning(f"Event missing application_id: {event}")
                consumer.commit()
                continue

            result = service.process(app_id)
            if result:
                try:
                    producer.send("screening.completed", value=result)
                    producer.flush()
                except Exception as e:
                    log.warning(f"Kafka publish failed for {app_id}: {e}")

            consumer.commit()
        except Exception as e:
            log.error(f"Error processing message: {e}", exc_info=True)
            time.sleep(1)


if __name__ == "__main__":
    run_consumer()
