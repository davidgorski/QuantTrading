#!/bin/bash
# shellcheck disable=SC2317
# Don't warn about unreachable commands in this file

set -Eeo pipefail

echo "*************************************************************************"
echo ".> Getting secrets"
echo "*************************************************************************"

###############################################################################
#####		Get Secrets
###############################################################################


if [ "$TRADING_MODE" == "live" ]; then
  SECRET_ID="prod/quant_trader"
else
  SECRET_ID="qa/quant_trader"
fi

secret_value=$(aws secretsmanager get-secret-value --secret-id $SECRET_ID --query SecretString --output text)

# Check if the secret value is fetched successfully
if [ $? -eq 0 ]; then
  echo "Secret fetched successfully."

  # Parse and export specific keys only
  export TWS_USERID=$(echo $secret_value | jq -r '.TWS_USERID')
  export TWS_PASSWORD=$(echo $secret_value | jq -r '.TWS_PASSWORD')

  echo "TWS_USERID=$(echo $secret_value | jq -r '.TWS_USERID')"
  echo "TWS_PASSWORD=$(echo $secret_value | jq -r '.TWS_PASSWORD')"

  # Confirm the environment variables are set
  echo "Environment variables set from AWS Secrets Manager"

else
  echo "Failed to fetch secret value."
fi

echo "Done getting secrets"
