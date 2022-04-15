# Login using OAuth

Allows login using an OAuth2 provider. Currently supported providers are Google, Facebook and Linked-In OAuth2. Login can be done by using OAuth2 access token or OAuth2 authorization code and redirect URI. On success, provides a JWT access token that can be used to access protected resources and a refresh token.


## Request

**URLs**: 

`/v1/auth/login/google`

`/v1/auth/login/facebook`

`/v1/auth/login/linkedit`

**Method**: `POST`

**Query Parameters**: None

**Headers**:

`Accept: application/json`

**Content**:

`accessToken` - OAuth2 access token (required only if `code` and `redirectUri` not specified).

`code` - OAuth2 authorization code (equired only if `accessToken` not specified).

`redirectUri` - OAuth2 redirection URI code (required only if `accessToken` not specified).

**Content example (using OAuth2 access token)**:

```json
{
    "accessToken": "93144b288eb1fdccbe46d6fc0f241a51766ecd3d"
}
```
**Content example (using OAuth2 authorization code & redirect URI)**:

```json
{
    "code": "46d6fc0f293144b288eb1fdccbe41a51766ecd3d",
    "redirectUri": "https://somehost.com:3000/facebook"
}
```

## ✅ Success Response

**Code**: `200 OK`

**Headers**:

`Content-Type: application/json`

**Content**:

`accessToken` - JWT token usable to access protected resources

`refreshToken` - JWT token usable one-time only to refresh an expired `accessToken`.

**Content example**:

```json
{
    "accessToken": "93144b288eb1fdccbe46d6fc0f241a51766ecd3d",
    "refreshToken": "6d6fc0f241a51766ecd3d93144b288eb1fdccbe4"
}
```

## ❌ Error Responses

**Code**: `400 BAD REQUEST`

**Condition**: If missing `accessToken` and `code` or `redirectUri`.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 400,
    "description": "Bad Request",
    "cause": "missing redirectUri"
}
```

**Or**

**Code**: `409 CONFLICT`

**Condition**: If the user email is already registered for login using a different OAuth provider or login using password.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 409,
    "description": "Conclict",
    "cause": "Already exists: john.doe@gmail.com"
}
```

## See Also

[Login using Email/Password](LoginEmailPassword.md)

[Refresh JWT Access Token](RefreshJwt.md)

[Decode JWT Access Token](DecodeJwt.md)

[Request 2FA Code](Request2FACode.md)
