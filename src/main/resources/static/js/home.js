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
    try {
        const text = await fetchPokemonData(randomId);
        if ((!(text.startsWith('{')) || text.startsWith('['))) {throw new Error("Invalid or broken JSON.")}
        const data = JSON.parse(text);
        renderPokemon(data);
    } catch (err) {console.error(err);}
});

document.getElementById("search-button").addEventListener("click", search)
document.getElementById("search").addEventListener('change', search)
async function search() {
    const nameid = document.getElementById("search").value.trim();
    if (!nameid) return;
    try {
        const text = await fetchPokemonData(nameid);
        if (text.includes("Not Found")) {alert("Pokemon Not Found"); throw new Error("Pokemon not found.")}
        if (!(text.startsWith('{')) || text.startsWith('[')) {throw new Error("Invalid or broken JSON.")}   
        const data = JSON.parse(text);
        renderPokemon(data);
    } catch (err) {console.error(err);}
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

async function fetchPokemonData(nameid) {
    try {
        if (fetchPokemonLock) {return}   
        fetchPokemonLock = true
        const res = await fetch("/home/pokemon/" + nameid)
        return res.text()
    } finally {fetchPokemonLock = false}
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
        const res = await fetch("/home/team")
        return res.text();
    } finally {
        fetchTeamLock = false
    }
}

async function fetchUserData(value) {
    try {
        const res = await fetch("/getuserdata", {credentials: "include"});
        const data = await res.json();
        if (!data.loggedIn) {location.href = "/login";} else {
            if (value === "username") {return data.username}
            if (value === "pfpurl") {return data.pfplink}
            return null
        }
    } catch (err) {console.error("Error:", err);}
}

async function loadWebsite() {
    try {
        document.getElementById("username").innerHTML = await fetchUserData("username");
        document.getElementById("user-pfp-img").src = await fetchUserData("pfpurl");
        await loadTeam()
    } catch (err) {console.error("Error:", err);}
}




// Rework this to much bs for a function
async function loadTeam() {
    try {
        const text = await fetchTeamData();
        if (!(text.startsWith('{')) || text.startsWith('[')) {return}   
        const data = JSON.parse(text);

        const teamDiv = document.getElementById("team-div");
        if (!teamDiv) {
            console.error("Element with ID 'team-div' not found");
            return;
        }

        if (data.team_member_1 !== "0" && data.team_member_1 !== undefined) {
            const div = teamDiv.appendChild(document.createElement("div"));
            div.id = "pokemon-div-1";
            div.className = "pokemon-div"
            div.innerHTML = "<p>" + data.team_member_1 + "</p>";
        } 
        if (data.team_member_2 !== "0" && data.team_member_2 !== undefined) {
            const div = teamDiv.appendChild(document.createElement("div"));
            div.id = "pokemon-div-2";
            div.className = "pokemon-div"
            div.innerHTML = "<p>" + data.team_member_2 + "</p>";
        }
        if (data.team_member_3 !== "0" && data.team_member_3 !== undefined) {
            const div = teamDiv.appendChild(document.createElement("div"));
            div.id = "pokemon-div-3";
            div.className = "pokemon-div"
            div.innerHTML = "<p>" + data.team_member_3 + "</p>";
        }
        if (data.team_member_4 !== "0" && data.team_member_4 !== undefined) {
            const div = teamDiv.appendChild(document.createElement("div"));
            div.id = "pokemon-div-4";
            div.className = "pokemon-div"
            div.innerHTML = "<p>" + data.team_member_4 + "</p>";
        }
        if (data.team_member_5 !== "0" && data.team_member_5 !== undefined ) {
            const div = teamDiv.appendChild(document.createElement("div"));
            div.id = "pokemon-div-5";
            div.className = "pokemon-div"
            div.innerHTML = "<p>" + data.team_member_5 + "</p>";
        }
        if (data.team_member_6 !== "0" && data.team_member_6 !== undefined) {
            const div = teamDiv.appendChild(document.createElement("div"));
            div.id = "pokemon-div-6";
            div.className = "pokemon-div"
            div.innerHTML = "<p>" + data.team_member_6 + "</p>";
        }
        

    } catch (err) {
        console.error(err);
    }
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
            `<h4 id="name-header">Name: ${(data.name).replace("-", " ")} / ID: ${data.id}</h4>` +
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
            { max: 1025, name: "Gen IX"}
        ];

        let gen = "Gen X";
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
                        `<span class="stats-tooltip">    • ${
                            s.stat.name.replace(/-/g, " ")
                        }: ${s.base_stat}</span>`
                )
                .join("<br>");

        document.getElementById("pokemon-abilities").innerHTML =
            `<h4 id="abilities-header">Abilities: ${
                capitalizeFirstLetter(
                    data.abilities.map((a) => a.ability.name).join(", ")
            )}</h4>`;

        document.getElementById("pokemon-forms").innerHTML =
            `<h4 id="forms-header">Forms: ${data.forms
                .map((f) => f.name)
                .join(", ")}</h4>`;
        

        //show()
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







// Get moves tooltip functionality
async function getMovesData() {
    try {
        let x = event.clientX;
        let y = event.clientY;
        const element = document.elementFromPoint(x,y)
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

async function fetchMoves() {
    try {
        const raw = document.getElementById("pokemon-name").textContent.trim();
        const name = raw.replace("Name: ", "").split("/")[0].trim();

        const pokeRes = await fetchPokemonData(
            encodeURIComponent(name)
        );

        const pokeData = await JSON.parse(pokeRes)
        const movesList = pokeData.moves;

        document.getElementById("pokemon-moves").innerHTML =
            "<h4>Moves:</h4>" +
            movesList
                .map(
                    (m) =>
                        `<span class="move-tooltip">    • ${
                            m.move.name.replace(/-/g, " ")
                        }</span>`
                )
                .join("<br>");

        document.querySelectorAll(".move-tooltip").forEach((el) => {
            el.addEventListener("mouseover", () => { getMovesData() });
        });

    } catch (err) {
        console.error(err);
        alert("Error while fetching moves");
    }
}


await loadWebsite();
