# Sentio Systems - Makefile
# Run 'make' or 'make help' to see available commands

.PHONY: help setup up down build rebuild status restart logs health aitest \
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
	@echo "  $(GREEN)make health$(NC)             Check health of all services"
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
	@printf "  $(BOLD)Backend:$(NC)       " && (curl -sf http://localhost:8083/actuator/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Frontend:$(NC)      " && (curl -sf http://localhost:3000 > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Keycloak:$(NC)      " && (curl -sf http://localhost:8080/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Birder:$(NC)        " && (curl -sf http://localhost:8000/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)SpeciesNet:$(NC)    " && (curl -sf http://localhost:8081/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)Preprocessing:$(NC) " && (curl -sf http://localhost:8082/health > /dev/null && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)MediaMTX:$(NC)      " && (docker compose ps mediamtx --format '{{.Status}}' 2>/dev/null | grep -q healthy && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@printf "  $(BOLD)MQTT:$(NC)          " && (docker compose ps mqtt --format '{{.Status}}' 2>/dev/null | grep -q healthy && echo "$(GREEN)● healthy$(NC)" || echo "$(RED)○ offline$(NC)")
	@echo ""

aitest:
	@echo ""
	@echo "$(BOLD)$(CYAN)Testing AI Services...$(NC)"
	@echo ""
	@echo "$(YELLOW)Downloading test images...$(NC)"
	@curl -sf -o /tmp/test_bird.jpg "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Erithacus_rubecula_with_cocked_head.jpg/440px-Erithacus_rubecula_with_cocked_head.jpg"
	@curl -sf -o /tmp/test_cat.jpg "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/440px-Cat03.jpg"
	@echo ""
	@echo "$(BOLD)$(BLUE)Birder (bird image):$(NC)"
	@curl -sf -X POST -F "file=@/tmp/test_bird.jpg" http://localhost:8000/detect | jq -r '"  Species: \(.classification.top_species)\n  Confidence: \(.classification.top_confidence)"'
	@echo ""
	@echo "$(BOLD)$(BLUE)SpeciesNet (cat image):$(NC)"
	@curl -sf -X POST -F "file=@/tmp/test_cat.jpg" http://localhost:8081/detect | jq -r '"  Species: \(.classification.top_species | split(";") | last)\n  Confidence: \(.classification.top_confidence)"'
	@echo ""
	@echo "$(BOLD)$(GREEN)✓ AI tests complete$(NC)"

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
