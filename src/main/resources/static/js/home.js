var fetchPokemonLock = false
var fetchMoveLock = false
var fetchTeamLock = false

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

try {
    document.getElementById("add-to-team").addEventListener("click", async () => {
        const params = new URLSearchParams(window.location.search);
        const idFromUrl = params.get("id");
        let nameid = null;
        if (idFromUrl) {
            nameid = idFromUrl.trim();
        } else {
            console.error("No pokemon id found in input or URL");
            return;
        }

        try {
            const response = await fetch(`/team/add/${nameid}`);
            if (response.ok) {
                await loadTeam();
            } else {
                throw new Error("Failed to add team member");
            }
        } catch (err) {
            console.error(err);
        }
    });
} catch (err) {
    //console.error(err);
}

try {
    document.addEventListener("DOMContentLoaded", async () => {

        for (const el of document.getElementsByClassName("move")) {
            el.addEventListener("mouseover", async () => {
                const moveName = el.textContent.trim();

                try {
                    const res = await fetch(`/move?id=${encodeURIComponent(moveName)}`);
                    if (!res.ok) throw new Error("Fetch failed");

                    const data = await res.json();

                    let tooltipText = "";

                    if (data.move?.power != null) {
                        tooltipText += `Power: ${data.move.power}\n`;
                    }
                    if (data.move?.accuracy != null) {
                        tooltipText += `Accuracy: ${data.move.accuracy}\n`;
                    }
                    if (data.move?.moveType?.name != null) {
                        tooltipText += `Type: ${data.move.moveType.name}\n`;
                    }
                    if (data.move?.damageClass?.name != null) {
                        tooltipText += `Damage Class: ${data.move.damageClass.name}\n`;
                    }
                    if (data.move?.effect_chance != null) {
                        tooltipText += `Effect Chance: ${data.move.effect_chance}\n`;
                    }

                    if (tooltipText === "") { throw new Error("No data for tooltip"); }
                    el.title = tooltipText;

                } catch (err) {
                    console.error(`Error fetching data for ${moveName}:`, err);
                    el.title = "Could not load move data.";
                }
            }, { once: true });
        }
    });
} catch (err) {
    console.error(err);
}


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

// Get moves tooltip functionality
async function getMovesData(element) {
    try {
        if (element.textContent.includes("ID"))
            return
        const raw = element.textContent.trim();
        const name = raw.replace("• ", "",)
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
