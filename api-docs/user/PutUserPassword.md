# Put User Password

Allows to put (reset) the User password.


## Request

**URL**: `/v1/user/password`

**Method**: `PUT`

**Query Parameters**: None

**Headers**:

`Accept: application/json`

**Content**:

`email` - valid email (required).

`newPassword` - plaintext password (required).

`confirmPassword` - plaintext password confirmation (required).

`authCode ` - 2FA code (required).

**Content example**:

```json
{
    "email": "john.doe@gmail.com",
    "newPassword": "abc1@23",
    "confirmPassword": "abc@123",
    "authCode": "123456"
}
```

## ✅ Success Response

**Code**: `204 NO CONTENT`

**Headers**: None

## ❌ Error Responses

**Code**: `400 BAD REQUEST`

**Condition**: If a mandatory content field is missing.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 400,
    "description": "Bad Request",
    "cause": "Missing email"
}
```
**Or**

**Code**: `403 FORBIDDEN`

**Condition**: If 2FA failed because `authCode` is invalid.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 403,
    "description": "Forbidden",
    "cause": "2FA failed"
}
```

**Or**

**Code**: `422 UNPROCESSABLE ENTITY`

**Condition**: If a content field is malformed or invalid.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 422,
    "description": "Unprocessable Entity",
    "cause": "Invalid email: john.doe!gmail.com"
}
```
## See Also

[Put User (Add New User)](PutUser.md)

[Patch User (Update User)](PatchUser.md)

[Get User](GetUser.md)

[Delete User](DeleteUser.md)
