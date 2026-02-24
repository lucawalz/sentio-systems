# Sentio Systems - Makefile
# Run 'make' or 'make help' to see available commands

# E2E base URL — point at the Docker frontend (override if needed)
E2E_BASE_URL ?= http://localhost:3000

.PHONY: help setup up down build rebuild status restart logs \
        health test-backend test-backend-coverage test-frontend test-ai test-e2e test-unit test-all \
        clean cleanimg cleanall shell seed passwords

# Colors
GREEN  := \033[0;32m
YELLOW := \033[0;33m
BLUE   := \033[0;34m
CYAN   := \033[0;36m
RED    := \033[0;31m
BOLD   := \033[1m
NC     := \033[0m # No Color

# Default target
help:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║           Sentio Systems - Available Commands                 ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Setup:$(NC)"
	@echo "  $(GREEN)make setup$(NC)              Run initial environment setup"
	@echo "  $(GREEN)make passwords$(NC)          Show passwords from .env file"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Docker:$(NC)"
	@echo "  $(GREEN)make up$(NC)                 Start all services"
	@echo "  $(GREEN)make down$(NC)               Stop all services"
	@echo "  $(GREEN)make build$(NC)              Build all images"
	@echo "  $(GREEN)make rebuild$(NC)            Force rebuild (no cache)"
	@echo "  $(GREEN)make status$(NC)             Show container status"
	@echo "  $(GREEN)make restart$(NC)            Restart all services"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Logs:$(NC)"
	@echo "  $(GREEN)make logs$(NC)               Follow logs for all services"
	@echo "  $(GREEN)make logs s=backend$(NC)     Follow logs for specific service"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Testing:$(NC)"
	@echo "  $(GREEN)make health$(NC)             Health check all running services"
	@echo "  $(GREEN)make test-backend$(NC)       Run backend unit tests (Maven, fast)"
	@echo "  $(GREEN)make test-backend-coverage$(NC) Run backend tests + JaCoCo coverage check"
	@echo "  $(GREEN)make test-frontend$(NC)      Run frontend unit tests + coverage (Vitest)"
	@echo "  $(GREEN)make test-ai$(NC)            Run AI service pytest suites + HTTP smoke test"
	@echo "  $(GREEN)make test-e2e$(NC)           Run E2E tests (Playwright, needs 'make up')"
	@echo "  $(GREEN)make test-unit$(NC)          Run unit tests only - no stack required"
	@echo "  $(GREEN)make test-all$(NC)           Run every test suite (needs 'make up')"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Cleanup:$(NC)"
	@echo "  $(GREEN)make clean$(NC)              Stop services and remove volumes"
	@echo "  $(GREEN)make cleanimg$(NC)           Remove all project images"
	@echo "  $(GREEN)make cleanall$(NC)           $(RED)Full cleanup (everything)$(NC)"
	@echo ""

# =============================================================================
# Setup
# =============================================================================

setup:
	@echo "$(BOLD)$(BLUE)Running environment setup...$(NC)"
	@./setup-env.sh

passwords:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║                    Credentials from .env                      ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Database:$(NC)"
	@echo "  User:     $(GREEN)$$(grep POSTGRES_USER .env | cut -d'=' -f2)$(NC)"
	@echo "  Password: $(GREEN)$$(grep POSTGRES_PASSWORD .env | cut -d'=' -f2)$(NC)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)Keycloak:$(NC)"
	@echo "  Admin:    $(GREEN)$$(grep KEYCLOAK_ADMIN= .env | cut -d'=' -f2)$(NC)"
	@echo "  Password: $(GREEN)$$(grep KEYCLOAK_ADMIN_PASSWORD .env | cut -d'=' -f2)$(NC)"
	@echo ""
	@echo "$(BOLD)$(YELLOW)MQTT:$(NC)"
	@echo "  User:     $(GREEN)$$(grep MQTT_USERNAME .env | cut -d'=' -f2)$(NC)"
	@echo "  Password: $(GREEN)$$(grep MQTT_PASSWORD .env | cut -d'=' -f2)$(NC)"
	@echo ""

# =============================================================================
# Docker Commands
# =============================================================================

up:
	@echo "$(BOLD)$(GREEN)Starting all services...$(NC)"
	@docker compose up -d
	@echo "$(BOLD)$(GREEN)✓ All services started$(NC)"

down:
	@echo "$(BOLD)$(YELLOW)Stopping all services...$(NC)"
	@docker compose down
	@echo "$(BOLD)$(GREEN)✓ All services stopped$(NC)"

build:
	@echo "$(BOLD)$(BLUE)Building all images...$(NC)"
	@docker compose build
	@echo "$(BOLD)$(GREEN)✓ Build complete$(NC)"

rebuild:
	@echo "$(BOLD)$(BLUE)Rebuilding all images (no cache)...$(NC)"
	@docker compose build --no-cache
	@echo "$(BOLD)$(GREEN)✓ Rebuild complete$(NC)"

status:
	@echo ""
	@echo "$(BOLD)$(CYAN)Container Status:$(NC)"
	@echo ""
	@docker compose ps -a

restart:
	@echo "$(BOLD)$(YELLOW)Restarting all services...$(NC)"
	@docker compose restart
	@echo "$(BOLD)$(GREEN)✓ All services restarted$(NC)"

# =============================================================================
# Logs
# =============================================================================

logs:
ifdef s
	@echo "$(BOLD)$(BLUE)Following logs for: $(s)$(NC)"
	@docker compose logs -f $(s)
else
	@echo "$(BOLD)$(BLUE)Following all logs (Ctrl+C to exit)$(NC)"
	@docker compose logs -f
endif

# =============================================================================
# Testing & Health
# =============================================================================

health:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║                    Service Health Check                       ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@printf "  $(BOLD)Backend:$(NC)       " && (docker compose ps backend --format '{{.State}}' 2>/dev/null | grep -qi running && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Frontend:$(NC)      " && (curl -sf http://localhost:3000 > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Keycloak:$(NC)      " && (curl -sf http://localhost:8080/realms/sentio > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Birder:$(NC)        " && (curl -sf http://localhost:8000/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)SpeciesNet:$(NC)    " && (curl -sf http://localhost:8081/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Preprocessing:$(NC) " && (curl -sf http://localhost:8082/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)MediaMTX:$(NC)      " && (docker compose ps mediamtx --format '{{.State}}' 2>/dev/null | grep -qi running && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)MQTT:$(NC)          " && (docker compose ps mosquitto --format '{{.State}}' 2>/dev/null | grep -qi running && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Postgres:$(NC)      " && (docker compose ps postgres --format '{{.State}}' 2>/dev/null | grep -qi running && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Redis:$(NC)         " && (docker compose ps redis --format '{{.State}}' 2>/dev/null | grep -qi running && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@echo ""

# --- Backend ---
test-backend:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║              Backend Tests (Maven)                            ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(BOLD)$(BLUE)Running unit + integration tests...$(NC)"
	@cd sentio-backend && ./mvnw test
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ Backend tests complete$(NC)"

# --- Backend tests + JaCoCo coverage gate ---
test-backend-coverage:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║              Backend Tests + Coverage Gate                    ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(BOLD)$(BLUE)Running tests with JaCoCo coverage enforcement...$(NC)"
	@echo "$(YELLOW)Required: 50%% instruction coverage, 35%% branch coverage (target: 80%%/75%%)$(NC)"
	@cd sentio-backend && ./mvnw verify
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ Coverage gate passed$(NC)"

# --- Frontend unit tests ---
test-frontend:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║              Frontend Tests (Vitest)                          ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(BOLD)$(BLUE)Running unit tests with coverage...$(NC)"
	@cd sentio-web && npm run test:coverage
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ Frontend tests complete$(NC)"

# --- Frontend E2E tests (needs stack running via 'make up') ---
test-e2e:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║              Frontend E2E Tests (Playwright)                   ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@printf "  Checking frontend at $(E2E_BASE_URL)... " && \
		(curl -sf $(E2E_BASE_URL) > /dev/null && echo "$(GREEN)● reachable$(NC)" || \
		(echo "$(RED)○ not reachable$(NC)"; echo "$(RED)Run 'make up' first$(NC)"; exit 1))
	@echo ""
	@cd sentio-web && BASE_URL=$(E2E_BASE_URL) CI=true npm run test:e2e
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ E2E tests complete$(NC)"

# --- AI service pytest suites + HTTP smoke test ---
test-ai:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║              AI Service Tests                                 ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(BOLD)$(BLUE)── Birder unit tests ──$(NC)"
	@docker compose exec -T birder python3 -m pytest tests/ -v
	@echo ""
	@echo "$(BOLD)$(BLUE)── SpeciesNet unit tests ──$(NC)"
	@docker compose exec -T speciesnet python3 -m pytest tests/ -v
	@echo ""
	@echo "$(BOLD)$(BLUE)── Preprocessing unit tests ──$(NC)"
	@docker compose exec -T preprocessing python3 -m pytest tests/ -v
	@echo ""
	@echo "$(BOLD)$(BLUE)── HTTP smoke test (needs stack up) ──$(NC)"
	@echo "$(YELLOW)Generating test images...$(NC)"
	@python3 -c "from PIL import Image; img=Image.new('RGB',(64,64),(34,139,34)); img.save('/tmp/test_bird.jpg','JPEG')" 2>/dev/null || \
		python3 -c "import struct,zlib; open('/tmp/test_bird.jpg','wb').write(bytes([0xff,0xd8,0xff,0xe0,0,16,74,70,73,70,0,1,1,0,0,1,0,1,0,0,0xff,0xd9]))"
	@python3 -c "from PIL import Image; img=Image.new('RGB',(64,64),(100,149,237)); img.save('/tmp/test_cat.jpg','JPEG')" 2>/dev/null || \
		python3 -c "open('/tmp/test_cat.jpg','wb').write(bytes([0xff,0xd8,0xff,0xe0,0,16,74,70,73,70,0,1,1,0,0,1,0,1,0,0,0xff,0xd9]))"
	@if [ -f /tmp/test_bird.jpg ] && [ -f /tmp/test_cat.jpg ]; then \
		printf "  $(BOLD)Birder health:$(NC)     " && \
			(curl -sf http://localhost:8000/health > /dev/null && echo "$(GREEN)● online$(NC)" || (echo "$(RED)○ offline - skipping$(NC)")); \
		if curl -sf http://localhost:8000/health > /dev/null 2>&1; then \
			echo "$(BOLD)$(BLUE)  Birder detect (bird):$(NC)"; \
			curl -sf -X POST -F "file=@/tmp/test_bird.jpg" http://localhost:8000/detect \
				| jq -r '"    Species:    " + .classification.top_species + "\n    Confidence: " + (.classification.top_confidence | tostring)' \
				|| echo "$(RED)    ✗ Unexpected response format$(NC)"; \
		fi; \
		printf "  $(BOLD)SpeciesNet health:$(NC) " && \
			(curl -sf http://localhost:8081/health > /dev/null && echo "$(GREEN)● online$(NC)" || (echo "$(RED)○ offline - skipping$(NC)")); \
		if curl -sf http://localhost:8081/health > /dev/null 2>&1; then \
			echo "$(BOLD)$(BLUE)  SpeciesNet detect (cat):$(NC)"; \
			curl -sf -X POST -F "file=@/tmp/test_cat.jpg" http://localhost:8081/detect \
				| jq -r '"    Species:    " + (.classification.top_species | split(";") | last) + "\n    Confidence: " + (.classification.top_confidence | tostring)' \
				|| echo "$(RED)    ✗ Unexpected response format$(NC)"; \
		fi; \
	else \
		echo "$(YELLOW)⚠ Skipping HTTP smoke test (image download failed - no internet?)$(NC)"; \
	fi
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ AI tests complete$(NC)"

# --- Unit tests only (no stack required) ---
test-unit:
	@$(MAKE) test-backend
	@$(MAKE) test-frontend

# --- Full test suite (needs 'make up') ---
test-all:
	@echo ""
	@echo "$(BOLD)$(CYAN)╔═══════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BOLD)$(CYAN)║              Full Test Suite                                  ║$(NC)"
	@echo "$(BOLD)$(CYAN)╚═══════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@$(MAKE) test-backend
	@$(MAKE) test-frontend
	@$(MAKE) test-ai
	@$(MAKE) test-e2e
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ All test suites complete$(NC)"

# =============================================================================
# Cleanup
# =============================================================================

clean:
	@echo "$(BOLD)$(YELLOW)Stopping services and removing volumes...$(NC)"
	@docker compose down -v
	@echo "$(BOLD)$(GREEN)✓ Cleanup complete$(NC)"

cleanimg:
	@echo "$(BOLD)$(YELLOW)Removing project images...$(NC)"
	@docker compose down --rmi local
	@echo "$(BOLD)$(GREEN)✓ Images removed$(NC)"

cleanall:
	@echo ""
	@echo "$(BOLD)$(RED)⚠️  Full cleanup - This will remove:$(NC)"
	@echo "    - All containers and volumes"
	@echo "    - All unused Docker images"
	@echo "    - All build cache"
	@echo ""
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	@echo ""
	@echo "$(BOLD)$(YELLOW)Running full cleanup...$(NC)"
	@docker compose down -v
	@docker system prune -af --volumes
	@docker builder prune -af
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ Full cleanup complete$(NC)"

# =============================================================================
# Development
# =============================================================================

shell:
ifdef s
	@docker compose exec $(s) /bin/sh
else
	@echo "$(YELLOW)Usage: make shell s=<service>$(NC)"
	@echo "$(YELLOW)Example: make shell s=backend$(NC)"
endif

seed:
	@echo "$(BOLD)$(BLUE)Seeding database with mock data...$(NC)"
	@./seed-mock-data.sh
	@echo "$(BOLD)$(GREEN)✓ Seeding complete$(NC)"
