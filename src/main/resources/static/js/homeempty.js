let fetchTeamLock = false;

document.getElementById("user-name").addEventListener("click", async () => {
    try {
        location.href = "/user";
    } catch (err) {console.error(err);}
})

document.getElementById("random-button").addEventListener("click", async () => {
    const randomId = Math.floor(Math.random() * 1025) + 1;
    window.location.href = `/pokemon?id=${randomId}`;
});

document.getElementById("search-button").addEventListener("click", search)
document.getElementById("search").addEventListener('change', search)
async function search() {
    const nameid = document.getElementById("search").value.trim();
    if (!nameid) return;
    window.location.href = `/pokemon?id=${nameid}`;
};

document.getElementById("user-logout").addEventListener("click", async () => {
    try {
        const response = await fetch("/logout", {method: "GET", credentials: "include"})
        if (response.ok) {
            location.href = "/login"
        } else {throw new Error("Logout failed")}
    } catch (err) {
        console.error(err)
    }
})

async function fetchTeamData() {
    try {
        if (fetchTeamLock) {return}
        fetchTeamLock = true
        const res = await fetch("/team")
        return res.text();
    } finally {
        fetchTeamLock = false
    }
}

async function loadWebsite() {
    try {
        await loadTeam()
    } catch (err) {console.error("Error:", err);}
}

async function loadTeam() {
    try {
        const text = await fetchTeamData();
        const teamElements = document.getElementsByClassName("inner-field-box");
        if (teamElements.length > 0) {
            for (let i = teamElements.length - 1; i >= 0; i--) {
                teamElements[i].parentNode.removeChild(teamElements[i]);
            }
        }
        const emptyMessages = document.getElementsByClassName("empty-team-message");
        if (emptyMessages.length > 0) {
            for (let i = emptyMessages.length - 1; i >= 0; i--) {
                emptyMessages[i].parentNode.removeChild(emptyMessages[i]);
            }
        }
        if (!(text.startsWith('{')) || text.startsWith('[')) {return}
        const data = JSON.parse(text);
        const teamDiv = document.getElementById("team-content");
        if (!teamDiv) {throw Error ("Element with ID 'team-content' not found");}
        if (data.team_members.length === 0) {
            const p = document.createElement("p")
            p.className = "empty-team-message"
            p.append("Nothing here yet")
            teamDiv.appendChild(p)
            return
        }
        if (teamDiv.firstChild.className === "empty-team-message") {
            teamDiv.removeChild(teamDiv.firstChild)
        }
        for (let i = 0; i < 6; i++) {
            if (data.team_members[i] !== 0 && data.team_members[i] != null && data.team_members[i] !== undefined && data.team_members[i] !== "") {
                const div = document.createElement("div");
                div.className = "inner-field-box"
                div.id = `team-member-${i}`
                const p = document.createElement("p")
                p.className = "team-member"
                p.append(data.team_members[i])
                div.appendChild(p)
                div.addEventListener("click", () => {
                    window.location.href = `/pokemon?id=${data.team_members[i]}`
                })
                const image = document.createElement("img")
                image.className = "team-member-remove-button"
                image.src = "images/delete.svg"
                image.alt = "✖"
                image.addEventListener("click", async (event) => {
                    event.stopPropagation()
                    await removeTeamMember(data.team_members[i]);
                    loadTeam();
                })
                div.appendChild(image)
                teamDiv.appendChild(div)
            }
        }
    } catch (err) {console.error(err)}
}

async function removeTeamMember(pokemonId) {
    try {
        const response = await fetch(`/team/remove/${pokemonId}`)
    } catch (err) {
        console.error(err)
    }
}

await loadWebsite();
