prefix    = "pagopa"
env       = "dev"
env_short = "d"
location  = "westeurope"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "pagoPA"
  Source      = "https://github.com/pagopa/pagopa-gpd-upload"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

cd_github_federations = [
  {
    repository = "gpd-upload-service"
    subject    = "dev-cd"
  }
]
