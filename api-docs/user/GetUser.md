# Get User

Allows to get an User.


## Request

**URLs**:

`/v1/user/me`

`/v1/user/{userId}`

**Method**: `GET`

**Query Parameters**: None

**Headers**:

`Accept: application/json`

## ✅ Success Response

**Code**: `200 OK`

**Headers**:

`Authorization: Bearer {accessToken}`

`Content-Type: application/json`

**Content**:

`id` - unique User identifier

`firstName` - first name (only if available).

`lastName` - last name (only if available).

`nickName` - nickname (only if available).

`pictureUrl ` - valid URL of image file (only if available).

`role` - user role (only if available).

`genres` - list of genres (only if available).

`email` - valid email (only if requester is the same as target User).

**Content example**:

```json
{
    "id": "7bd2862f-8deb-4814-8943-156d9dab80dd",
    "firstName": "John",
    "lastName": "Doe",
    "nickName": "Johnny"
    "pictureUrl": "https://somewebsite/john-doe.png",
    "role": "Producer",
    "genres": ["Pop", "Hip Hop", "R&B"],
    "email": "john.doe@gmail.com"
}
```
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

[Put User (Add New User)](PutUser.md)

[Patch User (Update User)](PatchUser.md)

[Delete User](DeleteUser.md)

[Put User Password (Reset Password)](PutUserPassword.md)
