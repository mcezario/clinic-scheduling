.PHONY: build
.DEFAULT_GOAL: build

start-database:
	docker compose -f infrastructure/local/docker-compose.yaml up -d;

wait-for-database:
	@echo "Waiting for database to be healthy..."
	@until [ "$$(docker inspect --format='{{.State.Health.Status}}' db 2>/dev/null)" = "healthy" ]; do \
		echo "Still waiting..."; \
		sleep 2; \
	done
	@echo "database is ready!"

start-app: start-database
	@$(MAKE) wait-for-database
	./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local

run-tests:
	./mvnw clean test

flyway-migrate:
	./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--spring.main.banner-mode=off --spring.main.web-application-type=none";
