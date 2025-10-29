// home.js - client-side logic for the Pokémon site
// Light cleanup: comments, spacing and grouping for readability.
// Functionality intentionally unchanged.

var fetchPokemonLock = false;
var fetchMoveLock = false;
var fetchTeamLock = false;

// -----------------------------
// Top-level event listeners
// -----------------------------

// Navigate to user page when username clicked
document.getElementById("user-name").addEventListener("click", async () => {
    try {
        location.href = "/user";
    } catch (err) { console.error(err); }
});

// Load a random Pokémon
document.getElementById("random-button").addEventListener("click", async () => {
    const randomId = Math.floor(Math.random() * 1025) + 1;
    try {
        await loadPokemon(randomId);
    } catch (err) {
        window.location.href = `/pokemon?id=${randomId}`;
    }
});

// Search input + button trigger
document.getElementById("search-button").addEventListener("click", search);
document.getElementById("search").addEventListener('change', search);
async function search() {
    const nameid = document.getElementById("search").value.trim();
    if (!nameid) return;
    try {
        await loadPokemon(nameid);
    } catch (err) {
        window.location.href = `/pokemon?id=${nameid}`;
    }
}

// Logout button
document.getElementById("user-logout").addEventListener("click", async () => {
    try {
        const response = await fetch("/logout", { method: "GET", credentials: "include" });
        if (response.ok) {
            location.href = "/login";
        } else {
            throw new Error("Logout failed");
        }
    } catch (err) {
        console.error(err);
    }
});

// On load: initialize certain UI pieces
try {
    await loadAddToTeamButton();
    document.addEventListener("DOMContentLoaded", async () => {
        await loadMoveTooltips();
    });
} catch (err) {
    console.error(err);
}

// -----------------------------
// Helpers: UI / color indicators
// -----------------------------

/**
 * Apply color styling to elements with .type-indicator based on type name text.
 * This is purely presentational and uses a fixed mapping.
 */
async function loadTypeColorIndicators() {
    document.querySelectorAll(".type-indicator").forEach(el => {
        const txt = el.textContent.toLowerCase();
        if (txt.includes("psychic")) {
            el.style.color = "#F95587";
        } else if (txt.includes("fire")) {
            el.style.color = "#EE8130";
        } else if (txt.includes("water")) {
            el.style.color = "#6390F0";
        } else if (txt.includes("grass")) {
            el.style.color = "#7AC74C";
        } else if (txt.includes("electric")) {
            el.style.color = "#F7D02C";
        } else if (txt.includes("ice")) {
            el.style.color = "#96D9D6";
        } else if (txt.includes("dragon")) {
            el.style.color = "#6F35FC";
        } else if (txt.includes("dark")) {
            el.style.color = "#705746";
        } else if (txt.includes("fairy")) {
            el.style.color = "#D685AD";
        } else if (txt.includes("normal")) {
            el.style.color = "#A8A77A";
        } else if (txt.includes("fighting")) {
            el.style.color = "#C22E28";
        } else if (txt.includes("flying")) {
            el.style.color = "#A98FF3";
        } else if (txt.includes("poison")) {
            el.style.color = "#A33EA1";
        } else if (txt.includes("ground")) {
            el.style.color = "#E2BF65";
        } else if (txt.includes("rock")) {
            el.style.color = "#B6A136";
        } else if (txt.includes("bug")) {
            el.style.color = "#A6B91A";
        } else if (txt.includes("ghost")) {
            el.style.color = "#735797";
        } else if (txt.includes("steel")) {
            el.style.color = "#B7B7CE";
        }
    });
}

// -----------------------------
// Team management: add/remove/load
// -----------------------------

/**
 * Wire up the "Add to Team" button. Reads current URL id and calls the server.
 */
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

/**
 * Fetch and render team details (size, types, weaknesses and average stats).
 */
async function loadTeamDetails() {
    try {
        const res = await fetch("/team-api");
        if (!res.ok) throw new Error("Fetch failed");
        const data = await res.json();

        document.getElementById("team-size").innerText = `Team Size: ${data.teamSize} / 6`;

        // Types
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

        // Weaknesses
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

        // Color indicators after rendering
        await loadTypeColorIndicators();

        // Stats
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

// -----------------------------
// Move tooltips and move-related helpers
// -----------------------------

/**
 * Attach a one-time mouseover handler to each .move element that fills title with move info.
 */
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

// Return raw move JSON or null on failure
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

// Get aggregated move types by Pokémon id (server endpoint)
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

// -----------------------------
// Fetch helpers with locks to prevent concurrent requests
// -----------------------------

async function fetchTeamData() {
    try {
        if (fetchTeamLock) { return; }
        fetchTeamLock = true;
        const res = await fetch("/team");
        return res.text();
    } finally {
        fetchTeamLock = false;
    }
}

async function fetchPokemonData(nameid) {
    try {
        if (fetchPokemonLock) { return; }
        fetchPokemonLock = true;
        const res = await fetch("/pokemon-api?id=" + nameid);
        return res.text();
    } finally {
        fetchPokemonLock = false;
    }
}

// -----------------------------
// Page load and Pokémon rendering
// -----------------------------

async function loadWebsite() {
    try {
        await loadTeam();
        await loadTeamDetails();
        await loadMoveTooltips();

        // Load Pokémon id from URL and render it
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
    } catch (err) {
        console.error("Error:", err);
    }
}

/**
 * Render the Pokémon page using data from the server.
 * Keeps existing DOM structure and IDs intact.
 */
async function loadPokemon(id) {
    try {
        const text = await fetchPokemonData(id);
        const data = await JSON.parse(text);

        // Determine generation label based on id
        const generations = [
            { max: 151, name: "Gen I" },
            { max: 251, name: "Gen II" },
            { max: 386, name: "Gen III" },
            { max: 493, name: "Gen IV" },
            { max: 649, name: "Gen V" },
            { max: 721, name: "Gen VI" },
            { max: 809, name: "Gen VII" },
            { max: 905, name: "Gen VIII" },
        ];
        let gen = "Gen IX";
        for (const g of generations) { if (data.pokemon.id <= g.max) { gen = g.name; break; } }

        // Populate DOM elements (preserve same markup as original)
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

        // Moves table
        document.getElementById("pokemon-moves").innerHTML = "<h4 id=\"move-header\">Moves:" + "<table>" + data.pokemon.moves.map((m) => `<tr><td class="move">${(m.inner.name).replace("-", " ")}</td></tr>`).join('') + "</table>" + "</h4>";

        // Reuse the URL id if present for move types query
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

        // Attach move tooltips and color indicators after rendering DOM
        await loadMoveTooltips();
        await loadTypeColorIndicators();

        // Update browser URL and re-attach the add-to-team button
        const newUrl = `/pokemon?id=${data.pokemon.id}`;
        window.history.pushState(null, '', newUrl);
        await loadAddToTeamButton();
    } catch (err) {
        console.error("Error loading Pokemon:", err);
    }
}

// -----------------------------
// Team list rendering
// -----------------------------

async function loadTeam() {
    try {
        const text = await fetchTeamData();

        // Remove any existing team members from DOM
        const teamElements = document.getElementsByClassName("inner-field-box");
        if (teamElements.length > 0) {
            for (let i = teamElements.length - 1; i >= 0; i--) {
                teamElements[i].parentNode.removeChild(teamElements[i]);
            }
        }

        // Remove any empty team messages
        const emptyMessages = document.getElementsByClassName("empty-team-message");
        if (emptyMessages.length > 0) {
            for (let i = emptyMessages.length - 1; i >= 0; i--) {
                emptyMessages[i].parentNode.removeChild(emptyMessages[i]);
            }
        }

        if (!text) { return; }
        if (text.trim() === "") { return; }
        if (text.trim() === "null") { return; }
        if (text.trim() === "undefined") { return; }

        // Validate text looks like an object (preserve original logic)
        if (!(text.startsWith('{')) || text.startsWith('[')) { return; }

        const data = JSON.parse(text);
        const teamDiv = document.getElementById("team-content");
        if (!teamDiv) { throw Error ("Element with ID 'team-content' not found"); }

        if (data.team_members.length === 0) {
            const p = document.createElement("p");
            p.className = "empty-team-message";
            p.append("Nothing here yet");
            teamDiv.appendChild(p);
            return;
        }

        if (teamDiv.firstChild && teamDiv.firstChild.className === "empty-team-message") {
            teamDiv.removeChild(teamDiv.firstChild);
        }

        let teraAssigned = false;

        // Render up to 6 team slots
        for (let i = 0; i < 6; i++) {
            if (data.team_members[i] !== 0 && data.team_members[i] != null && data.team_members[i] !== undefined && data.team_members[i] !== "") {
                const div = document.createElement("div");
                div.className = "inner-field-box";
                div.id = `team-member-${i}`;

                const p = document.createElement("p");
                p.className = "team-member";
                p.append(data.team_members[i]);
                div.appendChild(p);

                div.addEventListener("click", () => {
                    loadPokemon(data.team_members[i]);
                });
                const image = document.createElement("img");
                image.className = "team-member-remove-button";
                image.src = "images/delete.svg";
                image.alt = "✖";
                image.addEventListener("click", async (event) => {
                    event.stopPropagation();
                    await removeTeamMember(data.team_members[i]);
                    loadTeam();
                    await loadTeamDetails();
                });

                const dropdown = document.createElement("select");
                dropdown.className = "tera-type-dropdown";
                const defaultOption = document.createElement("option");

                let response = null;
                try {
                    response = await fetch(`/extra-api?id=${data.team_members[i]}`);
                } catch (err) {
                    console.error(err);
                }

                defaultOption.value = "";
                defaultOption.textContent = "None";

                dropdown.appendChild(defaultOption);
                const types = ["normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"];
                for (const type of types) {
                    const option = document.createElement("option");
                    option.value = type;
                    option.textContent = type.charAt(0).toUpperCase() + type.slice(1);
                    dropdown.appendChild(option);
                }

                if (response && response.ok) {
                    try {
                        const extraData = await response.json();
                        const teraType = extraData.extra.extra.teraType;
                        const teraPokemon = extraData.extra.extra.teraPokemon;
                        if (teraPokemon == data.team_members[i] && !teraAssigned) {
                            dropdown.value = teraType;
                            teraAssigned = true;
                        } else {
                            dropdown.value = "";

                        }
                    } catch (err) {
                        console.error(err);
                    }
                }

                dropdown.addEventListener("change", async () => {
                    try {
                        const selectedType = dropdown.value;
                        const pokemonId = data.team_members[i];
                        const response = await fetch(`/extra/set?id=${pokemonId}&type=${selectedType}`, { method: "POST" });
                        if (!response || !response.ok) {
                            dropdown.value = "";
                            return;
                        } else {
                            if (selectedType !== "") {
                                const all = document.getElementsByClassName("tera-type-dropdown");
                                for (const other of all) {
                                    if (other !== dropdown && other.value !== "") {
                                        other.value = "";
                                    }
                                }
                            }
                        }
                        loadTeamDetails();
                        loadTypeColorIndicators();
                    } catch (err) {
                        console.error(err);
                    }
                }, false);

                const controllDiv = document.createElement("div");
                controllDiv.className = "control-div";
                controllDiv.appendChild(dropdown);
                controllDiv.appendChild(image);
                div.appendChild(controllDiv);
                teamDiv.appendChild(div);
            }
        }
    } catch (err) { console.error(err); }
}

async function removeTeamMember(pokemonId) {
    try {
        const response = await fetch(`/team/remove/${pokemonId}`);
    } catch (err) {
        console.error(err);
    }
}

// -----------------------------
// Clipboard / export utilities
// -----------------------------

async function dumpSiteData() {
    const result = {};

    const params = new URLSearchParams(window.location.search);
    const idFromUrl = params.get("id");
    if (idFromUrl) {
        const nameid = idFromUrl.trim();

        try {
            const pokeRes = await fetch(`/pokemon-api?id=${encodeURIComponent(nameid)}`);
            result.pokemon = pokeRes.ok ? await pokeRes.json() : null;
        } catch (err) {
            console.error("Error fetching Pokémon:", err);
            result.pokemon = null;
        }
    }

    return result;
}

async function copySiteData() {
    try {
        const data = await dumpSiteData();
        const jsonStr = JSON.stringify(data, null, 2);
        await navigator.clipboard.writeText(jsonStr);
        console.log("Site data copied to clipboard!");
    } catch (err) {
        console.error("Failed to copy site data:", err);
    }
}

async function pasteSiteDataFromClipboard() {
    try {
        const clipboardText = await navigator.clipboard.readText();
        const data = JSON.parse(clipboardText);

        loadPokemon(data.pokemon.pokemon.id || 1);

        await loadMoveTooltips();
        await loadTypeColorIndicators();
        await loadAddToTeamButton();

        console.log("Page rebuilt from clipboard JSON");
    } catch (err) {
        console.error("Failed to paste site data from clipboard:", err);
    }
}

// -----------------------------
// CSV / Excel export helpers
// -----------------------------

function convertToCSV(jsonData) {
    let csv = "";

    if (jsonData.team?.team_members) {
        csv += "Team Members\n";
        jsonData.team.team_members.forEach((m, i) => {
            csv += `${i + 1},${m || ""}\n`;
        });
        csv += "\n";
    }

    if (jsonData.pokemon?.pokemon) {
        const p = jsonData.pokemon.pokemon;
        csv += "Pokemon Info\n";
        csv += `ID,${p.id}\n`;
        csv += `Name,${p.name}\n`;
        csv += `Height,${p.height}\n`;
        csv += `Weight,${p.weight}\n`;
        csv += `Types,${p.types.map(t => t.inner.name).join(", ")}\n`;
        csv += `Abilities,${p.abilities.map(a => a.inner.name).join(", ")}\n`;
        csv += "\n";

        csv += "Stats\n";
        csv += "Stat,Value\n";
        p.stats.forEach(s => {
            csv += `${s.inner.name.replace("-", " ")},${s.baseStat}\n`;
        });
        csv += "\n";

        csv += "Moves\n";
        csv += "Move,Power,Accuracy,Type,DamageClass,EffectChance\n";
        for (const moveName in jsonData.moves) {
            const m = jsonData.moves[moveName]?.move || {};
            csv += [
                moveName
            ].join(",") + "\n";
        }
        csv += "\n";
    }

    return csv;
}

function downloadCSV(jsonData, filename = "site_data.csv") {
    const csv = convertToCSV(jsonData);
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
}

function downloadExcel(jsonData, filename = "site_data.xlsx") {
    const wb = XLSX.utils.book_new();

    if (jsonData.pokemon?.pokemon) {
        const p = jsonData.pokemon.pokemon;

        const infoData = [
            ["ID", p.id],
            ["Name", p.name],
            ["Height", p.height],
            ["Weight", p.weight],
            ["Types", p.types.map(t => t.inner.name).join(", ")],
            ["Abilities", p.abilities.map(a => a.inner.name).join(", ")],
            ["Stats", p.stats.map(s => `${s.inner.name}: ${s.baseStat}`).join(", ")],
            ["Moves", p.moves.map(m => m.inner.name).join(", ")]
        ];
        const wsInfo = XLSX.utils.aoa_to_sheet(infoData);
        XLSX.utils.book_append_sheet(wb, wsInfo, "Pokemon Info");
    }

    XLSX.writeFile(wb, filename);
}

// -----------------------------
// UI buttons for clipboard/exports
// -----------------------------

document.getElementById("copy-pokemon").addEventListener("click", async () => {
    try {
        const params = new URLSearchParams(window.location.search);
        const idFromUrl = params.get("id");

        const pokemonRes = await fetch("/pokemon-api?id=" + encodeURIComponent(idFromUrl.trim()));
        const pokemon = await pokemonRes.json();

        const fullData = { pokemon };


        await navigator.clipboard.writeText(JSON.stringify(fullData, null, 2));
        alert("All site data copied successfully!");
    } catch (err) {
        console.error(err);
        alert("Failed to copy site data!");
    }
});

document.getElementById("paste-pokemon").addEventListener("click", async () => {
    try {
        const clipboardText = await navigator.clipboard.readText();
        const data = JSON.parse(clipboardText);

        // Load Pokémon data
        if (data.pokemon?.pokemon) {
            await loadPokemon(data.pokemon.pokemon.id);
        }

        alert("Site data pasted successfully!");
    } catch (err) {
        console.error(err);
        alert("Failed to paste site data!");
    }
});

document.getElementById("download-csv").addEventListener("click", async () => { downloadCSV(await dumpSiteData()); });

document.getElementById("download-excel").addEventListener("click", async () => { downloadExcel(await dumpSiteData()); });


await loadWebsite();
