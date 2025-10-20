async function setPFP(file) {
    input = document.getElementById("user-pfp-input")
    
    const curFiles = input.files;
    if (curFiles.length === 0) {
    } else {
        const formData = new FormData();
        formData.append("file", curFiles[0]);

        try {
            const { username, pfplink } = await getUsername();
            const response = await fetch("https://192.168.161.22:8081/user/" + username + "/uploadpfp", {
            method: "POST",
            body: formData
            });

            if (!response.ok) {
            throw new Error(`Upload failed: ${response.status}`);
            }
            window.location.reload();
            return await response.text();
        } catch (err) {
            console.error("Error:", err);
        }
    }
}


document.getElementById("form-submit-button").addEventListener("click", setPFP)


async function getUsername() {
    try {
        const response = await fetch("/getuserdata", {
            credentials: "include"
        });
        const data = await response.json();

        if (!data.loggedIn) {
            location.href = "/login";
        } else {
            return { username: data.username, pfplink: data.pfplink };
        }
    } catch (err) {
        console.error("Error:", err);
    }
}

async function setUsername() {
    try {
        const { username, pfplink } = await getUsername();
        document.getElementById("user-pfp-img").src = pfplink;
    } catch (err) {
        console.error("Error:", err);
    }
}

setUsername()
