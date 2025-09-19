var fetchPokemonLock = false
var fetchMoveLock = false
var fetchTeamLock = false

document.getElementById("user-name").addEventListener("click", async () => {
    try {
        location.href = ("/user/" + await fetchUserData("username"));
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

async function fetchMoveData(nameid) {
    try {
        if (fetchMoveLock) {return}
        fetchMoveLock = true
        const res = await fetch("/home/move/" + nameid);
        return res.text();
    } finally {
        fetchMoveLock = false
    }
}

async function fetchTeamData() {
    try {
        if (fetchTeamLock) {return}
        fetchTeamLock = true
        const res = await fetch("/home/team")
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
        if (!(text.startsWith('{')) || text.startsWith('[')) {return}   
        const data = JSON.parse(text);
        const teamDiv = document.getElementById("team-div");
        if (!teamDiv) {throw Error ("Element with ID 'team-div' not found");}
        for (let i = 0; i < 6; i++) {
            if (data.team_members[i] !== 0) {
                console.log(data.team_members[i])
                const div = document.createElement("div");
                div.className = "pokemon-div"
                const p = document.createElement("p")
                p.append(data.team_members[i])
                div.appendChild(p)
                teamDiv.appendChild(div)
            }
        }
    } catch (err) {console.error(err)}
}

// Get moves tooltip functionality
async function getMovesData(element) {
    try {
        if (element.textContent.includes("ID"))
            return
        console.debug(element.id)
        const raw = element.textContent.trim();
        const name = raw.replace("• ", "",)
        console.debug("Move name: ", name)
        const moveRes = await fetchMoveData(name)

        if (!moveRes.ok) return "";
        const moveData = await moveRes.json();
        let tooltipLines = [];
            if (moveData.id != null) tooltipLines.push(`ID: ${moveData.id}`);
            if (moveData.type?.name) tooltipLines.push(`Type: ${moveData.type.name}`);
            if (moveData.damage_class?.name) tooltipLines.push(`Damage Class: ${moveData.damage_class.name}`);
            if (moveData.accuracy != null) tooltipLines.push(`Accuracy: ${moveData.accuracy}`);

            const tooltipContent = tooltipLines.join("<br>");

        element.innerHTML = `<span class="tooltip">    • ${moveData.name}<span class="tooltiptext">${tooltipContent}</span></span>`;
    } catch (err) {
        console.error(err)
    } 
}



await loadWebsite();
