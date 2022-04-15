# Delete User

Deletes an existing User. Only allowed if the requestor is the same as the target User.


## Request

**URLs**:

`/v1/user/me`

`/v1/user/{myUserId}`

**Method**: `DELETE`

**Query Parameters**: None

**Headers**:

`Authorization: Bearer {accessToken}`


## ✅ Success Response

**Code**: `204 NO CONTENT`

**Headers**: None

## ❌ Error Responses

**Code**: `403 FORBIDDEN`

**Condition**: If requestor is not the same as target User.

## See Also

[Put User (Add New User)](PutUser.md)

[Patch User (Update User)](PatchUser.md)

[Get User](GetUser.md)

[Put User Password (Reset Password)](PutUserPassword.md)
