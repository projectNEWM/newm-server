# Put User

Allows to add new User.


## Request

**URL**: `/v1/user`

**Method**: `PUT`

**Query Parameters**: None

**Headers**:

`Accept: application/json`

**Content**:

`firstName` - first name (optional).

`lastName` - last name (optional).

`nickName` - nickname (optional).

`pictureUrl ` - valid URL of image file (optional).

`role` - user role (optional).

`genres` - list of genres (optional).

`email` - valid email (required).

`newPassword` - plaintext password (required).

`confirmPassword` - plaintext password confirmation (required).

`authCode ` - 2FA code (required).

**Content example**:

```json
{
    "firstName": "John",
    "lastName": "Doe",
    "nickName": "Johnny"
    "pictureUrl": "https://somewebsite/john-doe.png",
    "role": "Producer",
    "genres": ["Pop", "Hip Hop", "R&B"],
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

**Code**: `409 CONFLICT`

**Condition**: If `email` is already registered to another user.

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

[Patch User (Update User)](PatchUser.md)

[Get User](GetUser.md)

[Delete User](DeleteUser.md)

[Put User Password (Reset Password)](PutUserPassword.md)
