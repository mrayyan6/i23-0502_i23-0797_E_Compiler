# Makefile — json2xml translator (Java)
# CS-4031 Compiler Construction — Assignment 04
#
# Requirements: JDK 11+
#
# Targets:
#   make          — compile all Java sources
#   make run      — compile + run a quick demo
#   make test     — run all five test cases
#   make clean    — remove compiled classes and wrapper script
#   make jar      — package everything into json2xml.jar

JAVAC   = javac
JAVA    = java
SRC_DIR = src
BIN_DIR = bin
PKG     = json2xml
MAIN    = json2xml.Main

SOURCES := $(wildcard $(SRC_DIR)/$(PKG)/*.java)

.PHONY: all clean test jar run

# ─── Compile ────────────────────────────────────────────────────────────
all: $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(SOURCES)
	@echo "Build successful."
	@# Create a convenience wrapper script
	@echo '#!/bin/sh' > json2xml
	@echo 'exec java -cp "$(shell pwd)/bin" $(MAIN) "$$@"' >> json2xml
	@chmod +x json2xml
	@echo "Wrapper script './json2xml' created."

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

# ─── JAR ────────────────────────────────────────────────────────────────
jar: all
	jar cfe json2xml.jar $(MAIN) -C $(BIN_DIR) .
	@echo "JAR created: json2xml.jar"
	@echo "Run with:  java -jar json2xml.jar < input.json"

# ─── Tests ──────────────────────────────────────────────────────────────
test: all
	@PASS=0; FAIL=0; \
	for f in tests/*.json; do \
	    base=$$(basename $$f .json); \
	    expected="tests/$${base}.xml"; \
	    if [ ! -f "$$expected" ]; then \
	        echo "[SKIP] $$base (no expected XML)"; continue; \
	    fi; \
	    actual=$$(./json2xml < "$$f" 2>/dev/null); \
	    exp_content=$$(cat "$$expected"); \
	    if [ "$$actual" = "$$exp_content" ]; then \
	        echo "[PASS] $$base"; PASS=$$((PASS+1)); \
	    else \
	        echo "[FAIL] $$base"; \
	        echo "  Expected:"; echo "$$exp_content"; \
	        echo "  Got:";      echo "$$actual"; \
	        FAIL=$$((FAIL+1)); \
	    fi; \
	done; \
	echo ""; echo "Results: $$PASS passed, $$FAIL failed"

# ─── Quick demo ──────────────────────────────────────────────────────────
run: all
	@echo '{"name":"Ali","age":19,"active":true,"notes":null}' | ./json2xml

# ─── Clean ───────────────────────────────────────────────────────────────
clean:
	rm -rf $(BIN_DIR) json2xml json2xml.jar
