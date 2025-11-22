#!/usr/bin/env bash
#
# doctor.sh - Validate dependencies and optionally start services
#
# Usage:
#   ./doctor.sh           # Check dependencies only
#   ./doctor.sh --start   # Check dependencies and start both services
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track if all checks pass
ALL_PASSED=true

print_status() {
    if [ "$2" = "pass" ]; then
        echo -e "${GREEN}✓${NC} $1"
    elif [ "$2" = "warn" ]; then
        echo -e "${YELLOW}⚠${NC} $1"
    else
        echo -e "${RED}✗${NC} $1"
        ALL_PASSED=false
    fi
}

check_command() {
    if command -v "$1" &> /dev/null; then
        print_status "$2" "pass"
        return 0
    else
        print_status "$2" "fail"
        return 1
    fi
}

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║      ReliaQuest Employee API - Environment Doctor          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check dependencies
echo "Checking dependencies..."
echo ""

# Check for SDKMAN and activate correct Java version
if [ -f ".sdkmanrc" ] && [ -d "$HOME/.sdkman" ]; then
    print_status "SDKMAN detected with .sdkmanrc" "pass"

    # Source SDKMAN if not already loaded
    if ! command -v sdk &> /dev/null; then
        export SDKMAN_DIR="$HOME/.sdkman"
        if [ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
            source "$SDKMAN_DIR/bin/sdkman-init.sh"
        fi
    fi

    # Try to activate the environment from .sdkmanrc
    if command -v sdk &> /dev/null; then
        REQUIRED_JAVA=$(grep "^java=" .sdkmanrc | cut -d'=' -f2)
        if [ -n "$REQUIRED_JAVA" ]; then
            # Check if the required version is installed
            if [ -d "$HOME/.sdkman/candidates/java/$REQUIRED_JAVA" ]; then
                # Activate it
                sdk use java "$REQUIRED_JAVA" > /dev/null 2>&1
                print_status "  Activated Java $REQUIRED_JAVA via SDKMAN" "pass"
            else
                print_status "  Java $REQUIRED_JAVA not installed (run: sdk install java $REQUIRED_JAVA)" "fail"
            fi
        fi
    fi
elif [ -f ".sdkmanrc" ]; then
    print_status "SDKMAN not installed but .sdkmanrc exists" "warn"
    echo "        Install SDKMAN: https://sdkman.io/install"
fi

# Check Java
if check_command java "Java is installed"; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)

    if [ "$JAVA_MAJOR" -ge 25 ]; then
        print_status "  Java version: $JAVA_VERSION (≥25 required)" "pass"
    elif [ "$JAVA_MAJOR" -ge 17 ]; then
        print_status "  Java version: $JAVA_VERSION (25+ recommended, 17+ minimum)" "warn"
    else
        print_status "  Java version: $JAVA_VERSION (17+ required)" "fail"
    fi
fi

# Check Gradle wrapper
if [ -f "./gradlew" ]; then
    print_status "Gradle wrapper (gradlew) exists" "pass"

    if [ -x "./gradlew" ]; then
        print_status "  Gradle wrapper is executable" "pass"
    else
        print_status "  Gradle wrapper is not executable (run: chmod +x gradlew)" "fail"
    fi
else
    print_status "Gradle wrapper (gradlew) not found" "fail"
fi

# Check for required project files
echo ""
echo "Checking project structure..."
echo ""

if [ -d "api" ]; then
    print_status "API module directory exists" "pass"
else
    print_status "API module directory not found" "fail"
fi

if [ -d "server" ]; then
    print_status "Server module directory exists" "pass"
else
    print_status "Server module directory not found" "fail"
fi

if [ -f "api/src/main/java/com/reliaquest/api/ApiApplication.java" ]; then
    print_status "API main application class exists" "pass"
else
    print_status "API main application class not found" "fail"
fi

if [ -f "server/src/main/java/com/reliaquest/server/ServerApplication.java" ]; then
    print_status "Server main application class exists" "pass"
else
    print_status "Server main application class not found" "fail"
fi

# Check ports availability
echo ""
echo "Checking port availability..."
echo ""

check_port() {
    if lsof -i :"$1" &> /dev/null; then
        print_status "Port $1 is in use (required for $2)" "fail"
        return 1
    else
        print_status "Port $1 is available ($2)" "pass"
        return 0
    fi
}

check_port 8111 "API module"
check_port 8112 "Server module"

# Summary
echo ""
echo "────────────────────────────────────────────────────────────"

if [ "$ALL_PASSED" = true ]; then
    echo -e "${GREEN}All checks passed!${NC}"
else
    echo -e "${RED}Some checks failed. Please resolve the issues above.${NC}"
    if [ "$1" != "--start" ]; then
        exit 1
    fi
fi

# Start services if requested
if [ "$1" = "--start" ]; then
    echo ""
    echo "Starting services..."
    echo ""

    if [ "$ALL_PASSED" = false ]; then
        echo -e "${YELLOW}Warning: Starting services despite failed checks${NC}"
        echo ""
    fi

    # Start server in background
    echo "Starting Mock Employee API (server) on port 8112..."
    ./gradlew server:bootRun &
    SERVER_PID=$!

    # Wait for server to start
    echo "Waiting for server to be ready..."
    sleep 10

    # Check if server started successfully
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo -e "${RED}Server failed to start${NC}"
        exit 1
    fi

    # Start API in foreground
    echo ""
    echo "Starting Employee API on port 8111..."
    echo "Press Ctrl+C to stop both services"
    echo ""

    # Trap to kill server when API stops
    trap "echo ''; echo 'Stopping services...'; kill $SERVER_PID 2>/dev/null; exit 0" INT TERM

    ./gradlew api:bootRun

    # Clean up
    kill $SERVER_PID 2>/dev/null
else
    echo ""
    echo "To start both services, run:"
    echo "  ./doctor.sh --start"
    echo ""
    echo "Or start them manually:"
    echo "  Terminal 1: ./gradlew server:bootRun"
    echo "  Terminal 2: ./gradlew api:bootRun"
    echo ""
fi
