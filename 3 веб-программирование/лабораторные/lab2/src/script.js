const form = document.getElementById("point-form");
const xInput = document.getElementById("x-input");
const yFieldset = document.getElementById("y-fieldset");
const rFieldset = document.getElementById("r-fieldset");
const rInput = document.getElementById("r-input");
const xError = document.getElementById("x-error");
const yError = document.getElementById("y-error");
const rError = document.getElementById("r-error");
const resultsBody = document.getElementById("results-body");
const requestError = document.getElementById("request-error")
const storageKey = "lab2-results";
const allowedY = [-3, -2, -1, 0, 1, 2, 3, 4, 5];
const allowedR = [1, 2, 3, 4, 5];

const httpErrorMessages = {
    400: "Некорректный запрос.",
    401: "Данные для входа отсутствуют или не приняты сервером.",
    403: "Доступ к ресурсу запрещён.",
    404: "Запрашиваемый ресурс не найден.",
    405: "Этот HTTP-метод не разрешён для данного ресурса.",
    429: "Слишком много запросов. Попробуйте позже.",
    500: "Внутренняя ошибка сервера.",
    501: "Сервер не поддерживает функцию, необходимую для выполнения запроса.",
    502: "Веб-сервер получил некорректный ответ от Java-сервера.",
    503: "Сервис временно недоступен.",
    504: "Веб-сервер не дождался ответа от Java-сервера.",
    507: "На сервере недостаточно места для сохранения необходимых данных."
};

const results = loadResults();

function isXInRange(value) {
    if (typeof value !== "string" || !/^[+-]?\d+(\.\d+)?$/.test(value)) {
        return false;
    }

    const integerPart = Number(value.split(".")[0]);
    return Math.abs(integerPart) < 5;
}

function formatDateTime(checkedAt) {
    const timestamp = Date.parse(checkedAt);
    if (!Number.isFinite(timestamp)) {
        return "Некорректная дата";
    }

    return new Intl.DateTimeFormat("ru-RU", {
        dateStyle: "short",
        timeStyle: "medium"
    }).format(timestamp);
}

function addResultRow(result) {
    const row = resultsBody.insertRow();

    row.insertCell().textContent = result.x;
    row.insertCell().textContent = result.y;
    row.insertCell().textContent = result.r;

    const hitCell = row.insertCell();
    hitCell.textContent = result.hit ? "Попадание" : "Промах";
    hitCell.className = result.hit ? "hit" : "miss";

    row.insertCell().textContent = formatDateTime(result.checkedAt);
    row.insertCell().textContent= result.executionTimeNs ?? "-";
}

function isValidResult(result) {
    if (result === null || typeof result !== "object" || Array.isArray(result)) {
        return false;
    }

    return isXInRange(result.x)
        && allowedY.includes(result.y)
        && allowedR.includes(result.r)
        && typeof result.hit === "boolean"
        && typeof result.checkedAt === "string"
        && Number.isFinite(Date.parse(result.checkedAt))
        && (result.executionTimeNs === undefined
            || (Number.isInteger(result.executionTimeNs) && result.executionTimeNs >= 0));
}

function loadResults() {
    try {
        const savedResults = localStorage.getItem(storageKey);

        if (savedResults === null) {
            return [];
        }

        const parsedResults = JSON.parse(savedResults);
        if (!Array.isArray(parsedResults)) {
            throw new Error("Сохранённая история должна быть массивом.");
        }

        const validResults = parsedResults.filter(isValidResult);
        if (validResults.length !== parsedResults.length) {
            requestError.textContent = "Некоторые записи истории имеют неверный формат и не показаны.";
        }

        return validResults;
    } catch (error) {
        console.error(error);
        requestError.textContent = "Не удалось загрузить сохранённую историю";
        return [];
    }
}

function updateResults(savedResults) {
    results.length = 0;
    savedResults.forEach(function (result) {
        results.push(result);
    });

    resultsBody.replaceChildren();
    results.forEach(addResultRow);
    drawGraph();
}

function saveResults(result) {
    const savedResults = loadResults();
    savedResults.push(result);
    updateResults(savedResults);

    localStorage.setItem(storageKey, JSON.stringify(savedResults));
}

function synchronizeResults() {
    const savedResults = loadResults();
    if (JSON.stringify(savedResults) !== JSON.stringify(results)) {
        updateResults(savedResults);
    }
}

window.addEventListener("storage", function (event) {
    if (event.key !== storageKey && event.key !== null) {
        return;
    }

    synchronizeResults();
});

results.forEach(function (result) {
    addResultRow(result);
});

const clearHistoryButton = document.getElementById("clear-history-button");

clearHistoryButton.addEventListener("click", function () {
    if (!window.confirm("Удалить всю историю проверок? Это действие нельзя отменить.")) return;

    localStorage.removeItem(storageKey);
    results.length = 0;
    resultsBody.replaceChildren();
    drawGraph();
});

form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const data = new FormData(event.currentTarget);

        const x = data.get("x");
        const y = data.get("y");
        const r = data.get("r");

        requestError.textContent="";
        xError.textContent = "";
        yError.textContent = "";
        rError.textContent = "";
        xInput.classList.remove("invalid-field");
        yFieldset.classList.remove("invalid-field");
        rFieldset.classList.remove("invalid-field");

        let formIsValid = true;
        let normalizedX;
        let yNumber;
        let rNumber;

        if (typeof x !== "string" || x.trim() === "") {
            xError.textContent = "Введите X.";
            xInput.classList.add("invalid-field");
            formIsValid = false;
        } else {
            const xText = x.trim();
            const xPattern = /^[+-]?\d+([.,]\d+)?$/;

            if (!xPattern.test(xText)) {
                xError.textContent = "Введите X в правильном формате.";
                xInput.classList.add("invalid-field");
                formIsValid = false;
            } else {
                normalizedX = xText.replace(",", ".");
                if (!isXInRange(normalizedX)) {
                    xError.textContent = "X должен находиться строго между −5 и 5.";
                    xInput.classList.add("invalid-field");
                    formIsValid = false;
                }
            }
        }

        if (y === null || y === "") {
            yError.textContent = "Выберите Y.";
            yFieldset.classList.add("invalid-field");
            formIsValid = false;
        } else {
            yNumber = Number(y);
            if (!allowedY.includes(yNumber) || String(yNumber) !== y) {
                yError.textContent = "Недопустимое значение Y.";
                yFieldset.classList.add("invalid-field");
                formIsValid = false;
            }
        }

        if (r === null || r === "") {
            rError.textContent = "Выберите R.";
            rFieldset.classList.add("invalid-field");
            formIsValid = false;
        } else {
            rNumber = Number(r);
            if (!allowedR.includes(rNumber) || String(rNumber) !== r) {
                rError.textContent = "Недопустимое значение R.";
                rFieldset.classList.add("invalid-field");
                formIsValid = false;
            }
        }

        if (!formIsValid) {
            return;
        }
        const urls = new URLSearchParams({
            x: normalizedX,
            y: yNumber,
            r: rNumber

        });
        try {
            let response;
            const controller = new AbortController();
            try {
                response = await fetch("/check?" + urls.toString());
            }
            catch (e){

                requestError.textContent="Не удалось выполнить запрос к серверу";
                console.error(e);
                return;
            }
            const httpErrorMessage = "Ошибка HTTP: " + response.status + ". "
                + (httpErrorMessages[response.status] ?? "Не удалось выполнить запрос.");
            let result;
            try {
                result = await response.json();
            } catch (error) {
                if (!response.ok) {
                    requestError.textContent = httpErrorMessage;
                } else {
                    requestError.textContent = "Не удалось прочитать ответ сервера";
                }
                console.error(error);
                return;
            }

            if (!response.ok) {
                requestError.textContent = typeof result?.error === "string" && result.error.trim() !== ""
                    ? result.error : httpErrorMessage;
                return;
            }

            if (!isValidResult(result)) {
                requestError.textContent = "Сервер вернул результат в неверном формате.";
                return;
            }

            saveResults(result);
        } catch (err) {
            requestError.textContent = "Не удалось обработать результат проверки. Попробуйте обновить страницу.";
            console.error(err);
        }
    }
);



xInput.addEventListener("input", function () {
    xError.textContent = "";
    xInput.classList.remove("invalid-field");
});

yFieldset.addEventListener("change", function () {
    yError.textContent = "";
    yFieldset.classList.remove("invalid-field");
});

rInput.addEventListener("change", function () {
    rError.textContent = "";
    rFieldset.classList.remove("invalid-field");
    drawGraph();
});

function refreshResultTimes() {
    results.forEach(function (result, index) {
        resultsBody.rows[index].cells[4].textContent = formatDateTime(result.checkedAt);
    });
}

window.addEventListener("focus", function () {
    synchronizeResults();
    refreshResultTimes();
});
window.setInterval(synchronizeResults, 1000);
window.setInterval(refreshResultTimes, 30000);

function drawGraph() {
    const canvas = document.getElementById("graph");
    const ctx = canvas.getContext("2d");
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const selectedR = Number(rInput.value);
    const r = allowedR.includes(selectedR) ? selectedR : null;
    const scale = 28;
    const radius = (r === null ? 5 : r) * scale;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#3399ff";

    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.arc(centerX, centerY, radius / 2, -Math.PI / 2, 0);
    ctx.closePath();
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.lineTo(centerX + radius, centerY);
    ctx.lineTo(centerX, centerY + radius / 2);
    ctx.closePath();
    ctx.fill();

    ctx.fillRect(centerX - radius / 2, centerY, radius / 2, radius);

    ctx.strokeStyle = "#222222";
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(20, centerY);
    ctx.lineTo(canvas.width - 20, centerY);
    ctx.moveTo(centerX, canvas.height - 20);
    ctx.lineTo(centerX, 20);

    ctx.moveTo(canvas.width - 28, centerY - 5);
    ctx.lineTo(canvas.width - 20, centerY);
    ctx.lineTo(canvas.width - 28, centerY + 5);
    ctx.moveTo(centerX - 5, 28);
    ctx.lineTo(centerX, 20);
    ctx.lineTo(centerX + 5, 28);
    ctx.stroke();

    const marks = r === null ? [-5, -2.5, 2.5, 5] : [-5, -4, -3, -2, -1, 1, 2, 3, 4, 5];
    const labels = r === null ? ["−R", "−R/2", "R/2", "R"] : marks.map(String);
    ctx.fillStyle = "#222222";
    ctx.font = "14px sans-serif";
    ctx.textBaseline = "middle";

    marks.forEach(function (mark, index) {
        const x = centerX + mark * scale;
        const y = centerY - mark * scale;

        ctx.beginPath();
        ctx.moveTo(x, centerY - 4);
        ctx.lineTo(x, centerY + 4);
        ctx.moveTo(centerX - 4, y);
        ctx.lineTo(centerX + 4, y);
        ctx.stroke();

        ctx.textAlign = "center";
        ctx.fillText(labels[index], x, centerY + 18);
        ctx.textAlign = "left";
        ctx.fillText(labels[index], centerX + 10, y);
    });

    ctx.fillText("x", canvas.width - 20, centerY - 14);
    ctx.fillText("y", centerX + 10, 20);
    ctx.fillText("0", centerX - 15, centerY + 18);

    if (r === null) {
        return;
    }

    const points = results.filter(function (result) {
        return result.r === r;
    });

    points.forEach(function (result) {
        const x = centerX + Number(result.x) * scale;
        const y = centerY - Number(result.y) * scale;

        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.fillStyle = result.hit ? "green" : "#b00020";
        ctx.fill();
        ctx.strokeStyle = "white";
        ctx.lineWidth = 1;
        ctx.stroke();
    });
}

function moveParticle(particle, x, y, velocityX, velocityY, particleSize) {
    function move() {
        x += velocityX;
        y += velocityY;

        const rightWall = window.innerWidth - particleSize;
        const bottomWall = window.innerHeight - particleSize;

        if (x <= 0) {
            x = 0;
            velocityX = -velocityX;
        } else if (x >= rightWall) {
            x = rightWall;
            velocityX = -velocityX;
        }

        if (y <= 0) {
            y = 0;
            velocityY = -velocityY;
        } else if (y >= bottomWall) {
            y = bottomWall;
            velocityY = -velocityY;
        }

        particle.style.left = `${x}px`;
        particle.style.top = `${y}px`;

        requestAnimationFrame(move);
    }

    requestAnimationFrame(move);
}


document.addEventListener("click", function (event) {
    if (event.target !== document.body &&
        event.target !== document.documentElement) {
        return;
    }

    const particleCount = 12;

    for (let index = 0; index < particleCount; index++) {
        const particle = document.createElement("span");
        particle.className = "particle";
        document.body.append(particle);

        const particleSize = particle.offsetWidth;

        const x = Math.min(
            Math.max(event.clientX - particleSize / 2, 0),
            window.innerWidth - particleSize
        );

        const y = Math.min(
            Math.max(event.clientY - particleSize / 2, 0),
            window.innerHeight - particleSize
        );

        particle.style.left = `${x}px`;
        particle.style.top = `${y}px`;

        const angle = Math.PI * 2 * Math.random();
        const speed = 3;

        const velocityX = Math.cos(angle) * speed;
        const velocityY = Math.sin(angle) * speed;

        moveParticle(
            particle,
            x,
            y,
            velocityX,
            velocityY,
            particleSize
        );
    }
});

drawGraph();
