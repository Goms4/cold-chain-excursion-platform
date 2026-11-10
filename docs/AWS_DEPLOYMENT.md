# Simplest AWS Deployment

## Target
One EC2 instance with Docker Compose.

Containers:
- frontend
- backend
- postgres for the prototype

S3:
- analytical exports
- evidence artifacts

Security:
- expose only required HTTP/HTTPS/admin ports
- do not expose PostgreSQL publicly
- use environment variables/secrets
- least-privilege IAM

## Flow
GitHub -> GitHub Actions -> build/test Docker images -> deploy to EC2 ->
Docker Compose -> health verification.

## Rollback
Keep previous image/tag. On failed deployment:
1. stop new version
2. restore previous image
3. restart Compose
4. verify /actuator/health
5. run representative API request
