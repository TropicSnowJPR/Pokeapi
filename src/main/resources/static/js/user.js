async function setPFP() {
    const input = document.getElementById("user-pfp-input")

    const curFiles = input.files;
    if (curFiles.length === 0) {
    } else {
        const formData = new FormData();
        formData.append("file", curFiles[0]);

        try {
            const response = await fetch("/user/uploadpfp", {
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
