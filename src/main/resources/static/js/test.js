        let evolutionChain = await getEvolutionChain(nameid);
        console.log(evolutionChain);
        // Render the evolution chain into #pokemon-evolution-chain (if present)
        try {
            const evoContainer = document.getElementById("pokemon-evolution-chain");
            if (!evoContainer) {
                console.warn("No element with id 'pokemon-evolution-chain' found to render evolution chain.");
            } else if (!evolutionChain) {
                evoContainer.innerHTML = "<h4 id=\"evolution-header\">Evolution Chain: N/A</h4>";
            } else {
                // Support multiple possible shapes returned by the API, including:
                // - { evolution_chain: { chain: ... } }
                // - { chain: ... }
                // - raw chain object
                // - numeric-keyed map of chains like { "1": {...}, "2": {...} }
                let chainRoot = evolutionChain.evolution_chain?.chain ?? evolutionChain.chain ?? evolutionChain;

                // If chainRoot is a numeric-keyed map (e.g. { "1": {...}, "2": {...} }), try to pick
                // the chain that contains the currently viewed Pokémon (nameid), otherwise pick the first.
                function containsSpecies(node, targetName) {
                    if (!node) return false;
                    if (node.species && node.species.name && node.species.name.toLowerCase() === targetName) return true;
                    if (Array.isArray(node.evolvesTo)) {
                        for (const c of node.evolvesTo) {
                            if (containsSpecies(c, targetName)) return true;
                        }
                    }
                    return false;
                }

                if (chainRoot && typeof chainRoot === "object" && !Array.isArray(chainRoot)) {
                    const keys = Object.keys(chainRoot);
                    const numericKeys = keys.filter(k => String(Number(k)) === k);
                    // If numeric keyed mapping found, attempt to select the sub-chain containing this pokemon
                    if (numericKeys.length > 0) {
                        const target = String(nameid).toLowerCase();
                        let found = null;
                        for (const k of numericKeys) {
                            const candidate = chainRoot[k]?.chain ?? chainRoot[k];
                            if (candidate && containsSpecies(candidate, target)) { found = candidate; break; }
                        }
                        // fallback: maybe species names are stored at top-level values
                        if (!found) {
                            for (const k of numericKeys) {
                                const candidate = chainRoot[k];
                                if (candidate && containsSpecies(candidate, target)) { found = candidate; break; }
                            }
                        }
                        // final fallback: pick first numeric chain
                        chainRoot = found ?? (chainRoot[numericKeys[0]]?.chain ?? chainRoot[numericKeys[0]]);
                    }
                }

                // If chainRoot is an array (multiple chain entries), pick the one containing the pokemon or the first
                if (Array.isArray(chainRoot)) {
                    const target = String(nameid).toLowerCase();
                    let found = null;
                    for (const item of chainRoot) {
                        const candidate = item?.chain ?? item;
                        if (candidate && containsSpecies(candidate, target)) { found = candidate; break; }
                    }
                    chainRoot = found ?? (chainRoot[0]?.chain ?? chainRoot[0]);
                }

                // Recursive builder: returns nested <ul> structure
                function buildChain(node) {
                    if (!node || !node.species) return "";
                    const name = node.species.name || "";
                    let html = `<li><span class="evo-species" data-name="${name}">${name}</span>`;
                    if (Array.isArray(node.evolvesTo) && node.evolvesTo.length > 0) {
                        html += "<ul>";
                        for (const child of node.evolvesTo) {
                            html += buildChain(child);
                        }
                        html += "</ul>";
                    }
                    html += "</li>";
                    return html;
                }

                // If we still don't have a usable chainRoot, render N/A
                if (!chainRoot || (typeof chainRoot === "object" && !chainRoot.species && !chainRoot.chain && !Array.isArray(chainRoot))) {
                    evoContainer.innerHTML = "<h4 id=\"evolution-header\">Evolution Chain: N/A</h4>";
                } else {
                    const html = `<h4 id="evolution-header">Evolution Chain:</h4><div class="evolution-wrapper"><ul class="evolution-chain">${buildChain(chainRoot)}</ul></div>`;
                    evoContainer.innerHTML = html;

                    // Wire up clicks on species to load that Pokémon by name
                    for (const el of evoContainer.getElementsByClassName("evo-species")) {
                        el.style.cursor = "pointer";
                        el.addEventListener("click", async (evt) => {
                            const nm = evt.currentTarget.dataset.name;
                            if (nm) {
                                try { await loadPokemon(nm); } catch (err) { console.error(err); }
                            }
                        });
                    }
                }
            }
        } catch (err) {
            console.error("Error rendering evolution chain:", err);
        }