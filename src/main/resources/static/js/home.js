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
            } else {
                throw new Error("Failed to add team member");
            }
        } catch (err) {
            console.error(err);
        }
    });
}

async function loadMoveTooltips() {
    for (const el of document.getElementsByClassName("move")) {
        el.addEventListener("mouseover", async () => {
            const moveName = el.textContent.trim();

            try {
                const res = await fetch(`/move-api?id=${encodeURIComponent(moveName)}`);
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
    } catch (err) {console.error("Error:", err);}
}

async function loadPokemon(nameid) {
    try {
        const text = await fetchPokemonData(nameid);
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
        document.getElementById("pokemon-stats").innerHTML = "<h4 id=\"stats-header\">Stats:" + "<table>" + data.pokemon.stats.map((s) => `<tr><td>${s.inner.name}</td><td>${s.baseStat}</td></tr>`).join('') + "</table></h4>";
        document.getElementById("pokemon-abilities").innerHTML = `<h4 id="abilities-header">Abilities: ${data.pokemon.abilities.map((a) => a.inner.name).join(" ")}</h4>`;
        document.getElementById("pokemon-forms").innerHTML = `<h4 id="forms-header">Forms: ${data.pokemon.forms.map((f) => f.name).join(" ")}</h4>`;
        document.getElementById("latest-cry").src = data.pokemon.cries.latest || "./audio/default.ogg";
        document.getElementById("legacy-cry").src = data.pokemon.cries.legacy || "./audio/default.ogg";
        document.getElementById("pokemon-moves").innerHTML = "<h4 id=\"move-header\">Moves:" + "<table>" + data.pokemon.moves.map((m) => `<tr><td class="move">${m.inner.name}</td></tr>`).join('') + "</table>" + "</h4>";
        await loadMoveTooltips();
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
