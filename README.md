# Example project for Jenkins Pipeline for Rocket Workflow Promotions

This project will be deployed in customers that need to use the CICD pipeline to promote the Rocket Workflow from one environment to another.

# Prerequisites

It is assumed that you have the following prerequisites:
- A working Jenkins instance
- A working Rocket Workflow instance
- A working Rocket Workflow project
- A working Rocket Workflow environment
- A Jenkins secret containing the username and password for the Rocket Workflow instance (one per environment)
- A Jenkins node or `pod template` that has a working maven environment installed
- A maven settings filed stored in Jenkins and named: `NexusMultiRepoSettings`
- Optional: A file in Jenkins that contains a YAML with the structure
```
    "https://rocket.env.client/rocket/": "credential_env_id",
    "https://rocket.env2.client/rocket/": "credential_env2_id",
```