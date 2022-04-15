# Login using Email/Password

Allows login using email and password. On success, provides a JWT access token that can be used to access protected resources and a refresh token.

## Request

**URL**: `/v1/auth/login`

**Method**: `POST`

**Query Parameters**: None

**Headers**:

`Accept: application/json`

**Content**:

`email` - valid email address (required).

`password` - password in plaintext (required).

**Content example**:

```json
{
    "email": "john.doe@gmail.com",
    "password": "abcd1234"
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

**Code**: `404 NOT FOUND`

**Condition**: If no registered user with 'email' is found

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 404,
    "description": "Not Found",
    "cause": "Doesn't exist: john.doe@gmail.com"
}
```

**Or**

**Code**: `401 UNAUTHORIZED`

**Condition**: If 'password' is invalid.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 401,
    "description": "Unauthorized",
    "cause": "Invalid password"
}
```

## See Also

[Login using OAuth](LoginOAuth.md)

[Refresh JWT Access Token](RefreshJwt.md)

[Decode JWT Access Token](DecodeJwt.md)

[Request 2FA Code](Request2FACode.md)
