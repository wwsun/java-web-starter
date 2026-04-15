.PHONY: dev-db backend frontend up down test logs help

MVN_OPTS = -s .mvn/settings.xml

help: ## 显示所有可用命令
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

dev-db: ## 启动本地开发数据库（MySQL）
	docker compose up -d mysql

backend: ## 启动后端开发服务器（需先运行 dev-db，需 sdkman）
	cd backend && sdk env && mvn spring-boot:run $(MVN_OPTS)

frontend: ## 启动前端开发服务器
	cd frontend && npm install && npm run dev

up: ## 全栈 Docker 启动（生产模式，含 Redis）
	docker compose up -d --build

down: ## 停止所有 Docker 服务
	docker compose down

test: ## 运行前后端测试
	cd backend && mvn test $(MVN_OPTS)
	cd frontend && npm run test:run

logs: ## 查看后端容器日志
	docker compose logs -f backend
