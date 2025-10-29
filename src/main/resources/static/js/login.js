fetchSaltLock = false

async function fetchSalt(usernameoremail) {
    try {
        if (fetchSaltLock) {return}
        fetchSaltLock = true
        const res = await fetch(`/login/salt/${usernameoremail}`)
        return res.text();
    } finally {
        fetchSaltLock = false
    }
}

async function hashPassword(password, saltBase64) {
    const encoder = new TextEncoder();
    const passwordBytes = encoder.encode(password);
    const saltBytes = Uint8Array.from(atob(saltBase64), c => c.charCodeAt(0));

    const key = await crypto.subtle.importKey(
        'raw',
        passwordBytes,
        { name: 'PBKDF2' },
        false,
        ['deriveBits']
    );


    const derivedBits = await crypto.subtle.deriveBits(
        {
            name: 'PBKDF2',
            salt: saltBytes,
            iterations: 65536,
            hash: 'SHA-256'
        },
        key,
        256
    );

    const hashArray = new Uint8Array(derivedBits);
    let binary = '';
    hashArray.forEach(b => binary += String.fromCharCode(b));
    return btoa(binary);
}

document.getElementById('loginForm').addEventListener('submit', async e => {
    e.preventDefault();
    const usernameoremail = document.getElementById('usernameoremail').value;
    const password = document.getElementById('password').value;
    if (!usernameoremail || !password) {
        const errEl = document.getElementById('loginError');
        errEl.textContent = "⚠️ All fields are required.";
        errEl.hidden = false;
        setTimeout(() => {
            errEl.hidden = true;
            errEl.textContent = "";
        }, 5000);
    }
    const salt = await fetchSalt(usernameoremail);
    const data = JSON.parse(salt);
    if(!data.success) {
        const errEl = document.getElementById('loginError');
        errEl.textContent = "⚠️ " + (data.error || "Login failed, please try again.");
        errEl.hidden = false;
        setTimeout(() => {
            errEl.hidden = true;
            errEl.textContent = "";
        }, 5000);
        return;
    }
    const hashedPassword = await hashPassword(password, data.salt);

    fetch('/login/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ usernameoremail , password: hashedPassword })
    })
    .then(res => {
        if (!res.ok) throw new Error(`HTTP error! Status: ${res.status}`);
        return res.text();
    })
    .then(text => {
        let data;
        try {
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            console.error("Failed to parse JSON:", e, text);
            throw e;
        }

        console.log(data);
        if (data.success) {
            location.href='/'
        } else {
            const errEl = document.getElementById('loginError');
            errEl.textContent = "⚠️ " + (data.error || "Login failed, please try again.");
            errEl.hidden = false;
            setTimeout(() => {
                errEl.hidden = true;
                errEl.textContent = "";
            }, 5000);
        }
    })
    .catch(err => {
        console.error(err);
        const errEl = document.getElementById('loginError');
        errEl.textContent = "❌ Login failed due to a network error.";
        errEl.hidden = false;
        setTimeout(() => {
            errEl.hidden = true;
            errEl.textContent = "";
        }, 5000);
    });
});