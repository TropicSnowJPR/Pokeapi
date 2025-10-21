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
    const salt = await fetchSalt(usernameoremail);
    const data = JSON.parse(salt);
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
            alert("❌ Login failed: " + (data.error || "Unknown error")); // MAke a betta error system
        }
    })
    .catch(err => {
        console.error(err);
        alert("⚠️ Server error, please try again later."); //here to catch all errors
    });
});