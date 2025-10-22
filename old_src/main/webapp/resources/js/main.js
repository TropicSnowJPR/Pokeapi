document.getElementById("random-button").addEventListener("click", async () => {
    const randomId = Math.floor(Math.random() * 1025) + 1;

    try {
        const res = await fetch(`/pokemon?name=${randomId}`);
        if (!res.ok) {
            document.getElementById("pokemon-name").innerHTML =
                `<h4 id="name-header">Name: Pokemon not Found / ID: None</h4>` +
                '<input type="button" id="add-to-team" onclick="addToTeam()" value="Add to Team">';
            return;
        }
        const data = await res.json();
        renderPokemon(data);
    } catch (err) {
        console.error(err);
    }
});

document.getElementById("search-button").addEventListener("click", async () => {
    console.log(
        "Search button clicked with value:",
        document.getElementById("search").value
    );

    const name = document.getElementById("search").value.trim();
    if (!name) return;

    try {
        const res = await fetch(`/pokemon?name=${encodeURIComponent(name)}`);
        if (!res.ok) {
            document.getElementById("pokemon-name").textContent =
                "Name: Pokemon not found / ID: -1";
            return;
        }
        const data = await res.json();
        renderPokemon(data);
    } catch (err) {
        console.error(err);
    }
});

async function addToTeam() {
    const temp = document.getElementById("pokemon-name").textContent.split(": " && "/ ")[0]?.trim();
    const name = temp.replace("Name: ", "");

    if (!name || name === "Pokemon not found") {
        alert("Please search for a Pokémon first.");
        return;
    }

    if (await getTeamSize() >= 6) {
        alert("Your team is full! You can only have 6 Pokémon in your team.");
        return;
    }

    try {
        const res = await fetch(
            `/team/add?name=${encodeURIComponent(
                changeSpaceToMinus(uncapitalizeFirstLetter(name))
            )}`,
            { method: "POST" }
        );

        if (!res.ok) {
            alert("Failed to add Pokémon to team");
            return;
        }
        await loadTeam();
    } catch (err) {
        console.error(err);
        alert("Failed to add Pokémon to team");
    }
}

async function loadTeam() {
    try {
        const res = await fetch("/team");
        if (!res.ok) return;

        const data = await res.json();
        const teamContent = document.getElementById("team-content");
        teamContent.innerHTML = "";

        data.team.forEach((pokemon) => {
            const div = document.createElement("div");
            div.className = "poke-div";
            div.id = "pokemon-team-button";

            // Make the whole div clickable
            div.addEventListener("click", async () => {
                await fetchPokemon(pokemon);
            });

            const nameSpan = document.createElement("h4");
            nameSpan.textContent = changeMinusToSpace(
                capitalizeFirstLetter(pokemon)
            ).replace("- ", " ");

            const removeBtn = document.createElement("input");
            removeBtn.type = "button";
            removeBtn.value = "✖";
            removeBtn.className = "remove-team-btn";

            // Prevent removeBtn click from also triggering div click
            removeBtn.addEventListener("click", async (e) => {
                e.stopPropagation(); 
                await removeFromTeam(pokemon);
            });

            div.appendChild(nameSpan);
            div.appendChild(removeBtn);
            teamContent.appendChild(div);
        });
    } catch (err) {
        console.error(err);
    }
}


async function getTeamSize() {
    try {
        const res = await fetch("/team");
        if (!res.ok) return 0;

        const data = await res.json();
        return data.team.length;
    } catch (err) {
        console.error(err);
        return 0;
    }
}

async function removeFromTeam(name) {
    try {
        const res = await fetch(`/team/remove?name=${encodeURIComponent(name)}`, {
            method: "POST",
        });

        if (!res.ok) {
            alert("Failed to remove Pokémon from team");
            return;
        }
        await loadTeam();
    } catch (err) {
        console.error(err);
        alert("Failed to remove Pokémon from team");
    }
}

async function fetchPokemon(name) {
    try {
        if (fetchPokemonLock) {
            return
        }

        fetchPokemonLock = true
        name = name.replace("- ", "").trim();

        try {
            const res = await fetch(`/pokemon?name=${encodeURIComponent(name)}`);
            if (!res.ok) {
                document.getElementById("pokemon-name").textContent =
                    "Name: Pokemon not found / ID: -1";
                return;
            }
            const data = await res.json();
            renderPokemon(data);
        } catch (err) {
            console.error(err);
            document.getElementById("pokemon-name").textContent =
                "Name: Error fetching Pokémon";
            alert("Error fetching Pokémon data");
        }
    } catch (err) {
        console.error(err)
    } finally {
        fetchPokemonLock = false
    }
}

async function updatePage() {
    loadTeam();
    setTimeout(updatePage, 30000);
}

function renderPokemon(data) {
    try {

        document.getElementById("pokemon-official-artwork-image").src =
            data.sprites.other["official-artwork"].front_default ||
            "ball.png";

        document.getElementById("pokemon-pixel-artwork-image").src =
            data.sprites.front_default || 
            "ball.png";

        document.getElementById("pokemon-name").innerHTML =
            `<h4 id="name-header">Name: ${changeMinusToSpace(
                capitalizeFirstLetter(data.name)
            )} / ID: ${data.id}</h4>` +
            '<input type="button" id="add-to-team" onclick="addToTeam()" value="Add to Team">';

        document.getElementById("pokemon-height").innerHTML =
            `<h4 id="height-header">Height: ${data.height}</h4>`;

        document.getElementById("pokemon-weight").innerHTML =
            `<h4 id="weight-header">Weight: ${data.weight}</h4>`;

        document.getElementById("pokemon-types").innerHTML =
            `<h4 id="types-header">Types: ${data.types
                .map((t) => t.type.name)
                .join(", ")}</h4>`;

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
        for (const g of generations) {
            if (data.id <= g.max) {
                gen = g.name;
                break;
            }
        }

        document.getElementById("pokemon-generation").innerHTML =
            `<h4 id="generation-header">Generation: ${gen}</h4>`;

        document.getElementById("pokemon-stats").innerHTML =
            "<h4>Stats: </h4>" +
            data.stats
                .map(
                    (s) =>
                        `<span class="tooltip">    • ${changeMinusToSpace(
                            capitalizeFirstLetter(s.stat.name.replace(/-/g, " "))
                        )}: ${s.base_stat}</span>`
                )
                .join("<br>");

        document.getElementById("pokemon-abilities").innerHTML =
            `<h4 id="abilities-header">Abilities: ${changeMinusToSpace(
                capitalizeFirstLetter(
                    data.abilities.map((a) => a.ability.name).join(", ")
                )
            )}</h4>`;

        document.getElementById("pokemon-forms").innerHTML =
            `<h4 id="forms-header">Forms: ${data.forms
                .map((f) => f.name)
                .join(", ")}</h4>`;
        

        show()
        document.getElementById("latest-cry").src =
            data.cries.latest || "./audio/default.ogg";
        document.getElementById("legacy-cry").src =
            data.cries.legacy || "./audio/default.ogg";

        document.getElementById("latest-cry").volume = 0.2;
        document.getElementById("legacy-cry").volume = 0.2;

        fetchMoves();
    } catch (err) {
        console.error(err);
    }
}

function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

function uncapitalizeFirstLetter(string) {
    return string.charAt(0).toLowerCase() + string.slice(1);
}

function changeSpaceToMinus(string) {
    return string.replace(/ /g, "-");
}

function changeMinusToSpace(string) {
    return string.replace(/-/g, " ");
}

function show() {
  document.getElementById("latest-cry").style.display = "block";
  document.getElementById("legacy-cry").style.display = "block";
  document.getElementById("latest-cry-header").style.display = "block";
  document.getElementById("legacy-cry-header").style.display = "block";
}

async function copyText() {
    try {
        const temp = document.getElementById("pokemon-name").textContent.split(": " && "/ ")[0]?.trim();
        const name = temp.replace("Name: ", "");

        let text = await fetch(
            `/pokemon?name=${encodeURIComponent(
                uncapitalizeFirstLetter(changeSpaceToMinus(name))
            )}`,
            { method: "GET" }
        );

        if (!text.ok) {
            alert("Failed to Copy");
            return;
        }

        navigator.clipboard
            .writeText(JSON.stringify(await text.json()))
            .catch((err) => {
                console.error("Failed to copy: ", err);
            });
    } catch (err) {
        console.error("Failed to copy: ", err);
    }
}

async function enterJson() {
    try {
        const res = document.getElementById("json-input").value;
        const data = JSON.parse(res);
        renderPokemon(data);
    } catch (err) {
        console.error(err);
    }
}

async function fetchMoves() {
    try {
        const raw = document.getElementById("pokemon-name").textContent.trim();
        const name = raw.replace("Name: ", "").split("/")[0].trim();

        const pokeRes = await fetch(
            `/pokemon?name=${encodeURIComponent(
                uncapitalizeFirstLetter(changeSpaceToMinus(name))
            )}`
        );

        if (!pokeRes.ok) return alert("Failed to fetch Pokémon data");
        const pokeData = await pokeRes.json();
        const movesList = pokeData.moves;

        document.getElementById("pokemon-moves").innerHTML =
            "<h4>Moves:</h4>" +
            movesList
                .map(
                    (m) => 
                        `<span id="moves" class="tooltip" onmouseover="getMovesData()">    • ${changeMinusToSpace(
                                capitalizeFirstLetter(m.move.name.replace(/-/g, " "))
                            )}</span>`
            )
            .join("<br>");

    } catch (err) {
        console.error(err);
        alert("Error while fetching moves");
    }
}

async function getMovesData() {
    if (getMovesLock) {
        return
    }
    
    try {
        getMovesLock = true
        let x = event.clientX;
        let y = event.clientY;
        element = document.elementFromPoint(x,y)
        if (element.textContent.includes("ID"))
            return
        console.debug(element.id)
        const raw = element.textContent.trim();
        const name = changeSpaceToMinus(uncapitalizeFirstLetter(raw.replace("• ", "")))
        console.debug("Move name: ", name)
        const moveRes = await fetch(
            `/moves?name=${encodeURIComponent(name)}`
        );

        if (!moveRes.ok) return "";
        const moveData = await moveRes.json();
        let tooltipLines = [];
            if (moveData.id != null) tooltipLines.push(`ID: ${moveData.id}`);
            if (moveData.type?.name) tooltipLines.push(`Type: ${moveData.type.name}`);
            if (moveData.damage_class?.name) tooltipLines.push(`Damage Class: ${moveData.damage_class.name}`);
            if (moveData.accuracy != null) tooltipLines.push(`Accuracy: ${moveData.accuracy}`);

            const tooltipContent = tooltipLines.join("<br>");

        element.innerHTML = `<span class="tooltip">    • ${changeMinusToSpace(capitalizeFirstLetter(moveData.name))}<span class="tooltiptext">${tooltipContent}</span></span>`;
    } catch (err) {

    } finally {
        getMovesLock = false
    }
}


// Load team on page start
loadTeam();

let getMovesLock = false
let fetchPokemonLock = false

