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
    try {
        await loadPokemon(randomId);
    } catch (err) {
        window.location.href = `/pokemon?id=${randomId}`;
    }
});

document.getElementById("search-button").addEventListener("click", search)
document.getElementById("search").addEventListener('change', search)
async function search() {
    const nameid = document.getElementById("search").value.trim();
    if (!nameid) return;
    try {
        await loadPokemon(nameid);
    } catch (err) {
        window.location.href = `/pokemon?id=${nameid}`;
    }
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
    await loadAddToTeamButton();
    document.addEventListener("DOMContentLoaded", async () => {
        await loadMoveTooltips();
    });
} catch (err) {
    console.error(err);
}


async function loadTypeColorIndicators() {
    document.querySelectorAll(".type-indicator").forEach(el => {
        if (el.textContent.toLowerCase().includes("psychic")) {
            el.style.color = "#F95587";
        } else if (el.textContent.toLowerCase().includes("fire")) {
            el.style.color = "#EE8130";
        } else if (el.textContent.toLowerCase().includes("water")) {
            el.style.color = "#6390F0";
        } else if (el.textContent.toLowerCase().includes("grass")) {
            el.style.color = "#7AC74C";
        } else if (el.textContent.toLowerCase().includes("electric")) {
            el.style.color = "#F7D02C";
        } else if (el.textContent.toLowerCase().includes("ice")) {
            el.style.color = "#96D9D6";
        } else if (el.textContent.toLowerCase().includes("dragon")) {
            el.style.color = "#6F35FC";
        } else if (el.textContent.toLowerCase().includes("dark")) {
            el.style.color = "#705746";
        } else if (el.textContent.toLowerCase().includes("fairy")) {
            el.style.color = "#D685AD";
        } else if (el.textContent.toLowerCase().includes("normal")) {
            el.style.color = "#A8A77A";
        } else if (el.textContent.toLowerCase().includes("fighting")) {
            el.style.color = "#C22E28";
        } else if (el.textContent.toLowerCase().includes("flying")) {
            el.style.color = "#A98FF3";
        } else if (el.textContent.toLowerCase().includes("poison")) {
            el.style.color = "#A33EA1";
        } else if (el.textContent.toLowerCase().includes("ground")) {
            el.style.color = "#E2BF65";
        } else if (el.textContent.toLowerCase().includes("rock")) {
            el.style.color = "#B6A136";
        } else if (el.textContent.toLowerCase().includes("bug")) {
            el.style.color = "#A6B91A";
        } else if (el.textContent.toLowerCase().includes("ghost")) {
            el.style.color = "#735797";
        } else if (el.textContent.toLowerCase().includes("steel")) {
            el.style.color = "#B7B7CE";
        }
    });
}

async function loadAddToTeamButton() {
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
                await loadTeamDetails();
            } else {
                throw new Error("Failed to add team member");
            }
        } catch (err) {
            console.error(err);
        }
    });
}

async function loadTeamDetails() {
    try {
        const res = await fetch("/team-api");
        if (!res.ok) throw new Error("Fetch failed");
        const data = await res.json();
        document.getElementById("team-size").innerText = `Team Size: ${data.teamSize} / 6`;
        let typesHtml = "Team Types: <table>";
        if (Array.isArray(data.teamTypesUnique) && data.teamTypesUnique.length > 0) {
            for (const type of data.teamTypesUnique) {
                if (data.teamTypeCounts[type] !== null) {
                    typesHtml += `<tr><td>${data.teamTypeCounts[type]}x </td><td><span class="type-indicator">⬤ ${type.charAt(0).toUpperCase() + type.slice(1)}</span>`;
                } else {
                    typesHtml += `<tr><td>1x</td><td>${type}`;
                }
            }
        } else {
            typesHtml += "None";
        }
        typesHtml += "</table>";
        document.getElementById("team-types").innerHTML = typesHtml;

        let weaknessesHtml = "Team Weaknesses: <table>";
        if (Array.isArray(data.teamWeaknesses) && data.teamWeaknesses.length > 0) {
            for (const type of data.teamWeaknesses) {
                weaknessesHtml += `<tr><td><span class="type-indicator">⬤ ${type}</span></td></tr>`;
            }
        } else {
            weaknessesHtml += "None";
        }
        weaknessesHtml += "</table>";
        document.getElementById("team-weaknesses").innerHTML = weaknessesHtml;

        await loadTypeColorIndicators();

        let statsHtml = "Team Stats Average: ";
        if (Array.isArray(data.teamStats) && data.teamStats.length > 0) {
            statsHtml += `<table>${data.teamStats.map(stat => `<tr><td>${stat.name}</td><td>${stat.value}</td></tr>`).join("")}</table>`;
        } else if (data.teamStats && typeof data.teamStats === "object") {
            const entries = Object.entries(data.teamStats);
            if (entries.length > 0) {
                statsHtml += `<table>${entries.map(([k, v]) => `<tr><td>${k.replace("-", " ")}</td><td>${v}</td></tr>`).join("")}</table>`;
            } else {
                statsHtml += "N/A";
            }
        } else {
            statsHtml += "N/A";
        }
        document.getElementById("team-average-stats").innerHTML = statsHtml;
    } catch (err) {
        console.error(err);
    }
}

async function loadMoveTooltips() {
    for (const el of document.getElementsByClassName("move")) {
        el.addEventListener("mouseover", async () => {
            const moveName = el.textContent.trim();

            try {
                const res = await fetch(`/move-api?id=${encodeURIComponent(moveName.replace(" ", "-"))}`);
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
}

async function getMoveData(moveName) {
    try {
        const res = await fetch(`/move-api?id=${encodeURIComponent(moveName.replace(" ", "-"))}`);
        if (!res.ok) throw new Error("Fetch failed");

        const data = await res.json();
        return data;
    } catch (err) {
        console.error(`Error fetching data for ${moveName}:`, err);
        return null;
    }
}

async function getMoveTypes(id) {
    try {
        const res = await fetch(`/move-types-api?id=${encodeURIComponent(id)}`);
        if (!res.ok) throw new Error("Fetch failed");

        const data = await res.json();
        return data;
    } catch (err) {
        console.error(`Error fetching move types:`, err);
        return null;
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

async function fetchPokemonData(nameid) {
    try {
        if (fetchPokemonLock) {return}
        fetchPokemonLock = true
        const res = await fetch("/pokemon-api?id=" + nameid);
        return res.text();
    } finally {
        fetchPokemonLock = false
    }
}

async function loadWebsite() {
    try {
        await loadTeam()
        await loadTeamDetails()
        await loadMoveTooltips()
        const params = new URLSearchParams(window.location.search);
        const idFromUrl = params.get("id");
        let nameid = null;
        if (idFromUrl) {
            nameid = idFromUrl.trim();
        } else {
            console.error("No pokemon id found in input or URL");
            return;
        }
        await loadPokemon(nameid);
    } catch (err) {console.error("Error:", err);}
}

async function loadPokemon(id) {
    try {
        const text = await fetchPokemonData(id);
        const data = await JSON.parse(text);
        const generations = [{ max: 151, name: "Gen I" },{ max: 251, name: "Gen II" },{ max: 386, name: "Gen III" },{ max: 493, name: "Gen IV" },{ max: 649, name: "Gen V" },{ max: 721, name: "Gen VI" },{ max: 809, name: "Gen VII" },{ max: 905, name: "Gen VIII" },];
        let gen = "Gen IX";
        for (const g of generations) {if (data.pokemon.id <= g.max) {gen = g.name; break;}}
        document.getElementById("pokemon-name").innerHTML = `<h4 id="name-header">Name: ${data.pokemon.name} / ID: ${data.pokemon.id}</h4>` + '<button id="add-to-team" class="add-team-button">Add to Team</button>';
        document.getElementById("pokemon-official-artwork-image").src = data.pokemon.sprites.other.officialArtwork.frontDefault || "images/ball.png";
        document.getElementById("pokemon-pixel-artwork-image").src = data.pokemon.sprites.frontDefault || "images/ball.png";
        document.getElementById("pokemon-height").innerHTML = `<h4 id="height-header">Height: ${data.pokemon.height}</h4>`;
        document.getElementById("pokemon-weight").innerHTML = `<h4 id="weight-header">Weight: ${data.pokemon.weight}</h4>`;
        document.getElementById("pokemon-types").innerHTML = `<h4 id="types-header">Types: ${data.pokemon.types.map((t) => t.inner.name).join(", ")}</h4>`;
        document.getElementById("pokemon-generation").innerHTML = `<h4 id="generation-header">Generation: ${gen}</h4>`;
        document.getElementById("pokemon-stats").innerHTML = "<h4 id=\"stats-header\">Stats:" + "<table>" + data.pokemon.stats.map((s) => `<tr><td>${(s.inner.name).replace("-", " ")}</td><td>${s.baseStat}</td></tr>`).join('') + "</table></h4>";
        document.getElementById("pokemon-abilities").innerHTML = `<h4 id="abilities-header">Abilities: ${data.pokemon.abilities.map((a) => (a.inner.name).replace("-", " ")).join(", ")}</h4>`;
        document.getElementById("pokemon-forms").innerHTML = `<h4 id="forms-header">Forms: ${data.pokemon.forms.map((f) => f.name).join(" ")}</h4>`;
        document.getElementById("latest-cry").src = data.pokemon.cries.latest || "./audio/default.ogg";
        document.getElementById("legacy-cry").src = data.pokemon.cries.legacy || "./audio/default.ogg";
        document.getElementById("pokemon-moves").innerHTML = "<h4 id=\"move-header\">Moves:" + "<table>" + data.pokemon.moves.map((m) => `<tr><td class="move">${(m.inner.name).replace("-", " ")}</td></tr>`).join('') + "</table>" + "</h4>";
        const params = new URLSearchParams(window.location.search);
        const idFromUrl = params.get("id");
        let nameid = null;
        if (idFromUrl) {
            nameid = idFromUrl.trim();
        } else {
            console.error("No pokemon id found in input or URL");
            return;
        }
        const moveTypesData = await getMoveTypes(nameid);
        let moveTypesHtml = "<table>";
        for (const type in moveTypesData.moveTypeCounts) {
            if (moveTypesData.moveTypeCounts[type] !== null) {
                moveTypesHtml += `<tr><td>${moveTypesData.moveTypeCounts[type]}x </td><td><span class="type-indicator">⬤ ${type.charAt(0).toUpperCase() + type.slice(1)}</span></td></tr>`;
            }
        }
        moveTypesHtml += "</table>";

        document.getElementById("pokemon-move-types").innerHTML = `<h4 id="move-types-header">Move Types: ${moveTypesHtml}</h4>`;
        await loadMoveTooltips();
        await loadTypeColorIndicators();
        const newUrl = `/pokemon?id=${data.pokemon.id}`;
        window.history.pushState(null, '', newUrl);
        await loadAddToTeamButton();
    } catch (err) {
        console.error("Error loading Pokemon:", err);
    }
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
                    loadPokemon(data.team_members[i]);
                })
                const image = document.createElement("img")
                image.className = "team-member-remove-button"
                image.src = "images/delete.svg"
                image.alt = "✖"
                image.addEventListener("click", async (event) => {
                    event.stopPropagation()
                    await removeTeamMember(data.team_members[i]);
                    loadTeam();
                    await loadTeamDetails()
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
