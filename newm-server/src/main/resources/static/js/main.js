function onLoadHome() {
}

function onLogin() {
    axios.post('/login?session', {
        email: document.getElementById('email').value,
        password: document.getElementById('password').value
    })
    .then(function (response) {
        console.log(response);
        alert("Fake login using Post ok");
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onLogin2() {
    axios.get('/login2?session', {
        params: {
            email: document.getElementById('email2').value,
            password: document.getElementById('password2').value
        }
    })
    .then(function (response) {
        console.log(response);
        alert("Fake login using Get ok");
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onLogout() {
    axios.get('/logout', {
        withCredentials: true
    })
    .then(function (response) {
        console.log(response);
        location.reload();
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onJoin() {
    axios.put('/v1/users', {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        pictureUrl: document.getElementById('pictureUrl').value.nullIfEmpty(),
        email: document.getElementById('email').value,
        password: document.getElementById('password').value
    }, {
        params: { authCode: document.getElementById('authCode').value }
    })
    .then(function (response) {
        console.log(response);
        alert("Successfully Joined!!!")
        window.location.href = '/';
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onVerificationCode() {
    const email = document.getElementById('email').value
    axios.get('/auth/code', {
        params: { email: email }
    })
    .then(function (response) {
        console.log(response);
        alert("Verification Code successfully send to: " + email)
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onLoadProfile() {
    axios.get('/v1/users/me', {
        withCredentials: true
    })
    .then(function (response) {
        console.log(response);
        loadJsonTable('profileTable', response.data)
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onLoadJwt() {
    axios.get('/auth/jwt', {
        withCredentials: true
    })
    .then(function (response) {
        console.log(response);
        loadJsonTable('jwtTable', response.data)
    })
    .catch(function (error) {
        console.log(error);
        alert(error);
    });
}

function onLoadSongs() {
    axios.get('/v1/portal/songs', {
        withCredentials: true,
        transformResponse: body => body     // disable json parsing
    })
    .then(function (response) {
        console.log(response);
        document.getElementById('songsDiv').innerHTML = response.data
    })
    .catch(function (error) {
        console.log(error);
    });
}

function loadJsonTable(tableId, jsonData) {
    const table = document.getElementById(tableId);
    var row, cell1, cell2;
    for (let key in jsonData) {
        row = table.insertRow();
        cell1 = row.insertCell();
        cell2 = row.insertCell();
        cell1.innerHTML = key;
        cell2.innerHTML = jsonData[key];
    }
}

String.prototype.nullIfEmpty = function(str) {
    return  this == "" ? null : this;
}
