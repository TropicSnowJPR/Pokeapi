async function hashPassword(password, saltBase64) {
    const encoder = new TextEncoder();
    const passwordBytes = encoder.encode(password);
    const saltBytes = Uint8Array.from(atob(saltBase64), c => c.charCodeAt(0));

    // Import password as a CryptoKey
    const key = await crypto.subtle.importKey(
        'raw',
        passwordBytes,
        { name: 'PBKDF2' },
        false,
        ['deriveBits']
    );

    // Derive bits using PBKDF2 + HMAC-SHA256
    const derivedBits = await crypto.subtle.deriveBits(
        {
            name: 'PBKDF2',
            salt: saltBytes,
            iterations: 65536,
            hash: 'SHA-256'
        },
        key,
        256 // key length in bits (same as Java KEY_LENGTH)
    );

    // Convert to Base64 to match Java
    const hashArray = new Uint8Array(derivedBits);
    let binary = '';
    hashArray.forEach(b => binary += String.fromCharCode(b));
    return btoa(binary);
}

// Generate a random salt, Base64-encoded
function generateSalt() {
    const salt = crypto.getRandomValues(new Uint8Array(16));
    let binary = '';
    salt.forEach(b => binary += String.fromCharCode(b));
    return btoa(binary);
}

// Validate Password Security
function validatePassword(password) {
    const minLength = 8;
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasDigit = /\d/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);

    if (password.length < minLength) {
        return "Password must be at least 8 characters long.";
    }
    if (!hasUpper) {
        return "Password must contain at least one uppercase letter.";
    }
    if (!hasLower) {
        return "Password must contain at least one lowercase letter.";
    }
    if (!hasDigit) {
        return "Password must contain at least one digit.";
    }
    if (!hasSpecial) {
        return "Password must contain at least one special character.";
    }
    return null; // valid
}





document.getElementById('signupForm').addEventListener('submit', async e => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    if (!username || !email || !password) {
        const errEl = document.getElementById('signupError');
        errEl.textContent = "⚠️ All fields are required.";
        errEl.hidden = false;
        setTimeout(() => {
            errEl.hidden = true;
            errEl.textContent = "";
        }, 5000);
        return;
    };
    const errorMsg = validatePassword(password);
    if (errorMsg) {
        alert(errorMsg);
        return;
    }

    const salt = generateSalt();
    const hashedPassword = await hashPassword(password, salt);

    fetch('/signup/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password: hashedPassword, salt })
    })
    .then(res => {
        if (!res.ok) throw new Error(`HTTP error! Status: ${res.status}`);
        return res.text();  // parse as text first
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
            location.href='/login'
        } else {
            const errEl = document.getElementById('signupError');
            errEl.textContent = "⚠️ " + (data.error || "Signup failed, please try again.");
            errEl.hidden = false;
            setTimeout(() => {
                errEl.hidden = true;
                errEl.textContent = "";
            }, 5000);
        }
    })
    .catch(err => {
        console.error(err);
        const errEl = document.getElementById('signupError');
        errEl.textContent = "❌ Signup failed due to a network error.";
        errEl.hidden = false;
        setTimeout(() => {
            errEl.hidden = true;
            errEl.textContent = "";
        }, 5000);
    });
});