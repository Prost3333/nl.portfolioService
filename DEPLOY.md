# Бесплатный хостинг на Oracle Cloud (Always Free)

Стек проекта (3 Spring-сервиса + 3 PostgreSQL + Kafka + Zookeeper) требует ~3–4 GB RAM,
поэтому обычные бесплатные PaaS (Render, Railway, Fly.io, Koyeb) не подходят: там нет
бесплатной Kafka, а контейнеры засыпают без трафика — планировщик снапшотов в 18:00
не сработает.

Единственный бесплатный вариант, который тянет весь стек как есть — **Oracle Cloud
Always Free**: ARM-виртуалка **2 OCPU / 12 GB RAM / 50+ GB диск, бессрочно и бесплатно**
(лимит урезали с 4 OCPU/24 GB в июне 2026, но и половины хватает с запасом).

---

## 1. Регистрация в Oracle Cloud

1. Зарегистрируйтесь на <https://www.oracle.com/cloud/free/>.
2. Понадобится банковская карта — только для верификации, деньги не списываются.
   Аккаунт остаётся в режиме Free Tier, пока вы сами не апгрейднете его.
3. Выбирайте home region ближе к себе (например, `eu-amsterdam-1` или `eu-frankfurt-1`).
   Home region потом сменить нельзя.

## 2. Создание виртуальной машины

1. Console → **Compute → Instances → Create Instance**.
2. Image: **Ubuntu 24.04** (aarch64).
3. Shape: **Ampere → VM.Standard.A1.Flex**, выставьте **2 OCPU / 12 GB RAM**
   (это максимум Always Free).
4. Boot volume: 50–100 GB (Always Free даёт до 200 GB суммарно).
5. Скачайте/добавьте SSH-ключ, создайте инстанс, запишите публичный IP.

> Если видите **"Out of capacity"** — в регионе временно нет свободных ARM-машин.
> Попробуйте другую Availability Domain, другое время суток, или уменьшите до
> 1 OCPU / 6 GB (тоже хватит). Обычно за 1–3 дня попыток машина создаётся.

## 3. Открытие портов

Наружу нужны только порты сервисов: **8081, 8083, 8084** (базы и Kafka наружу не
публикуются — это закрыто в `docker-compose.prod.yml`).

**В облаке (Security List):**
Networking → Virtual Cloud Networks → ваша VCN → Subnet → Security List →
**Add Ingress Rules**: Source `0.0.0.0/0`, protocol TCP, destination ports `8081,8083,8084`.

**На самой VM (Oracle-образы Ubuntu имеют жёсткий iptables):**

```bash
sudo iptables -I INPUT 6 -p tcp --dport 8081 -j ACCEPT
sudo iptables -I INPUT 6 -p tcp --dport 8083 -j ACCEPT
sudo iptables -I INPUT 6 -p tcp --dport 8084 -j ACCEPT
sudo netfilter-persistent save
```

## 4. Установка Docker

```bash
ssh ubuntu@<PUBLIC_IP>
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
# перезайдите по ssh, чтобы группа применилась
```

## 5. Деплой проекта

```bash
git clone https://github.com/Prost3333/nl.portfolioService.git app
cd app

cp .env.example .env
nano .env   # задайте пароли БД и JWT_SECRET (openssl rand -base64 32)
            # при желании добавьте TZ=Europe/Amsterdam (таймзона крона снапшотов)

docker compose -f docker-compose.prod.yml up -d --build
```

Первая сборка на ARM занимает 10–15 минут (Gradle собирает все три сервиса).
Базовые образы `eclipse-temurin` мультиархитектурные — на ARM собираются без изменений.

Проверка:

```bash
docker compose ps
curl http://localhost:8081/auth/register -X POST -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"test1234"}'
```

Снаружи фронт/API доступны на `http://<PUBLIC_IP>:8084` (portfolio + фронтенд),
`:8081` (auth), `:8083` (report).

## 6. Автономная работа

- `restart: unless-stopped` (задано в `docker-compose.prod.yml`) — контейнеры сами
  поднимаются после падений и перезагрузки VM; Docker стартует при загрузке системы.
- Планировщик `SnapshotScheduler` (будни, 18:00) работает внутри portfolio-service —
  ничего дополнительно настраивать не нужно. Таймзона задаётся переменной `TZ`.
- Обновление после пуша в GitHub:

  ```bash
  cd ~/app && git pull && docker compose -f docker-compose.prod.yml up -d --build
  ```

- Логи: `docker compose logs -f portfolio-service`

## 7. Что можно улучшить потом (не обязательно)

- Повесить домен + HTTPS через Caddy (одна команда, автоматический Let's Encrypt).
- Бэкап томов PostgreSQL (`docker run --rm -v app_portfolio_data:/data ... tar`).
- CD: GitHub Actions по пушу в `main` делает ssh на VM и перезапускает compose.
