class ProvideUser{
   val userStr = """
    {
  "username": "mifos",
  "userId": 1,
  "base64EncodedAuthenticationKey": "bWlmb3M6cGFzc3dvcmQ=",
  "authenticated": true,
  "officeId": 1,
  "officeName": "Tristate Systems Ltd",
  "roles": [
    {
      "id": 1,
      "name": "Super user",
      "description": "This role provides all application permissions.",
      "disabled": false
    }
  ],
  "permissions": [
    "ALL_FUNCTIONS"
  ],
  "shouldRenewPassword": false,
  "isTwoFactorAuthenticationRequired": false
}
   """
}
