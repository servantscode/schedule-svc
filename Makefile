# import config.
# You can change the default config with `make cnf="config_special.env" build`
cnf ?= config.env
include $(cnf)
export $(shell sed 's/=.*//' $(cnf))

# import deploy config
# You can change the default deploy config with `make cnf="deploy_special.env" release`
dpl ?= deploy.env
include $(dpl)
export $(shell sed 's/=.*//' $(dpl))

# grep the version from the mix file
VERSION=$(shell cat version.txt)

# HELP
# This will output the help for each task
# thanks to https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.PHONY: help build

help: ## This help.
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help


# DOCKER TASKS
# Build the container
build: ## Build the container
	./inc-version.sh
	docker.exe build -t servantcode/$(APP_NAME) .

build-nc: ## Build the container without caching
	docker.exe build --no-cache -t servantcode/$(APP_NAME) .

run: ## Run container on port configured in `config.env`
	kubectl.exe create -f postgres-data-kube.yml
	kubectl.exe create -f postgres-kube.yml 
	kubectl.exe create -f kube.yml

up: build run ## Run container on port configured in `config.env` (Alias to run)

update:
	kubectl.exe set image deployment/$(APP_NAME) $(APP_NAME)=servantcode/$(APP_NAME):$(VERSION)

stop: ## Stop and remove a running container
	kubectl.exe delete -f kube.yml
	kubectl.exe delete -f postgres-kube.yml
	kubectl.exe delete -f postgres-data-kube.yml

major-release: bump-major-version build-nc publish 

minor-release: bump-minor-version build-nc publish 

release: bump-patch-version build-nc publish 

# Docker publish
publish: publish-latest publish-version 

bump-major-version:
	./inc-version.sh -M 

bump-minor-version:
	./inc-version.sh -m 

bump-patch-version:
	./inc-version.sh -p 

publish-latest: tag-latest 
	@echo 'publish latest to $(DOCKER_REPO)'
	docker.exe push $(DOCKER_REPO)/servantcode/$(APP_NAME):latest

publish-version: tag-version 
	@echo 'publish $(VERSION) to $(DOCKER_REPO)'
	docker.exe push $(DOCKER_REPO)/servantcode/$(APP_NAME):$(VERSION)

# Docker tagging
tag: tag-latest tag-version ## Generate container tags for the `{version}` ans `latest` tags

tag-latest: ## Generate container `{version}` tag
	@echo 'create tag latest'
	docker.exe tag servantcode/$(APP_NAME) $(DOCKER_REPO)/servantcode/$(APP_NAME):latest

tag-version: ## Generate container `latest` tag
	@echo 'create tag $(VERSION)'
	docker.exe tag servantcode/$(APP_NAME) $(DOCKER_REPO)/servantcode/$(APP_NAME):$(VERSION)

logs: ## Get logs from running container
	kubectl.exe logs $(shell kubectl.exe get pods | grep $(APP_NAME) | grep Running | cut -d ' ' -f 1)

# HELPERS
version: ## Output the current version
	@echo $(VERSION)
