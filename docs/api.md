API Endpoints
Аутентификация
- POST /api/auth/register       - Регистрация
- POST /api/auth/login          - Вход
- POST /api/auth/logout         - Выход
- POST /api/auth/refresh        - Обновление токена

Пользователи
- GET    /api/users/me          - Получить профиль текущего пользователя
- PUT    /api/users/me/password - Обновить пароль текущего пользователя
- DELETE /api/users/me          - Удалить аккаунт текущего пользователя

Администрирование пользователей (доступно для роли ROLE_ADMIN)
- PUT /api/admin/users/{id}/role - изменить роль пользователя

  Только пользователи с ролью ROLE_ADMIN могут изменять роли других пользователей.
  Все новые пользователи при регистрации получают роль ROLE_USER по умолчанию.
  Первого администратора необходимо создать вручную в базе.

Задачи
- GET    /api/tasks            - Получить все задачи
- GET    /api/tasks/{id}       - Получить задачу по ID
- POST   /api/tasks            - Создать задачу
- PUT    /api/tasks/{id}       - Обновить задачу
- DELETE /api/tasks/{id}       - Удалить задачу
- GET    /api/tasks/filter     - Фильтрация задач
- GET    /api/tasks/stats      - Статистика задач