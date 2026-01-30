запуск task-manager приложения

- сборка приложения
  mvn clean package

- запуск приложения (порт 9090) и базы (порт 5432)
  docker compose up --build

- UI
  Swagger UI: http://localhost:9090/swagger-ui/index.html
- проверка 
  HealthCheck: http://localhost:9090/actuator/health
