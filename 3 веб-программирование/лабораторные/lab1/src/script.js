const form = document.getElementById("point-form");
const xFieldset = document.getElementById("x-fieldset");
const yInput = document.getElementById("y-input");
const rFieldset = document.getElementById("r-fieldset");
const xError = document.getElementById("x-error");
const yError = document.getElementById("y-error");
const rError = document.getElementById("r-error");
const resultsBody = document.getElementById("results-body");
const storageKey = "lab1-results";
const allowedX = [-3, -2, -1, 0, 1, 2, 3, 4, 5];
const allowedR = [1, 2, 3, 4, 5];

const results = loadResults();

function isYInRange(value) {
    if (typeof value !== "string" || !/^[+-]?\d+(\.\d+)?$/.test(value)) {
        return false;
    }

    const integerPart = Number(value.split(".")[0]);
    return Math.abs(integerPart) < 3;
}

function isPointInArea(xNumber, yText, rNumber) {
    const parts = yText.split(".");
    const fractionPart = parts[1] || "";
    const scale = 10n ** BigInt(fractionPart.length);

    const x = BigInt(xNumber) * scale;
    const y = BigInt(parts[0] + fractionPart);
    const r = BigInt(rNumber) * scale;

    const inCircle =
        x <= 0n &&
        y <= 0n &&
        x * x + y * y <= r * r;

    const inTriangle =
        x >= 0n &&
        y >= 0n &&
        2n * y + x <= r;

    const inRectangle =
        x >= 0n &&
        x <= r &&
        2n * y >= -r &&
        y <= 0n;

    return inCircle || inTriangle || inRectangle;
}

function formatDateTime(checkedAt) {
    return new Intl.DateTimeFormat("ru-RU", {
        dateStyle: "short",
        timeStyle: "medium"
    }).format(new Date(checkedAt));
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
}

function loadResults() {
    const savedResults = localStorage.getItem(storageKey);

    if (savedResults === null) {
        return [];
    }

    return JSON.parse(savedResults);
}

function saveResults() {
    localStorage.setItem(storageKey, JSON.stringify(results));
}

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

form.addEventListener("submit", function (event) {
    event.preventDefault();

    const data = new FormData(event.currentTarget);

    const x = data.get("x");
    const y = data.get("y");
    const r = data.get("r");

    xError.textContent = "";
    yError.textContent = "";
    rError.textContent = "";
    xFieldset.classList.remove("invalid-field");
    yInput.classList.remove("invalid-field");
    rFieldset.classList.remove("invalid-field");

    let formIsValid = true;
    let xNumber;
    let normalizedY;
    let rNumber;

    if (x === null || x === "") {
        xError.textContent = "Выберите X.";
        xFieldset.classList.add("invalid-field");
        formIsValid = false;
    } else {
        xNumber = Number(x);
        if (!allowedX.includes(xNumber) || String(xNumber) !== x) {
            xError.textContent = "Недопустимое значение X.";
            xFieldset.classList.add("invalid-field");
            formIsValid = false;
        }
    }

    if (typeof y !== "string" || y.trim() === "") {
        yError.textContent = "Введите Y.";
        yInput.classList.add("invalid-field");
        formIsValid = false;
    } else {
        const yText = y.trim();
        const yPattern = /^[+-]?\d+([.,]\d+)?$/;

        if (!yPattern.test(yText)) {
            yError.textContent = "Введите Y в правильном формате.";
            yInput.classList.add("invalid-field");
            formIsValid = false;
        } else {
            normalizedY = yText.replace(",", ".");
            if (!isYInRange(normalizedY)) {
                yError.textContent = "Y должен находиться строго между −3 и 3.";
                yInput.classList.add("invalid-field");
                formIsValid = false;
            }
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

    const hit = isPointInArea(xNumber, normalizedY, rNumber);

    const result = {
        x: xNumber,
        y: normalizedY,
        r: rNumber,
        hit: hit,
        checkedAt: new Date().toISOString()
    };

    results.push(result);
    addResultRow(result);
    saveResults();
    drawGraph();
});

const xButtons = document.querySelectorAll(".x-button");
const xInput = document.getElementById("x-input");

xButtons.forEach(function (button) {
    button.addEventListener("click", function (event) {
        xButtons.forEach(function (otherButton) {
            otherButton.classList.remove("selected");
        });

        event.currentTarget.classList.add("selected");
        xInput.value = event.currentTarget.value;
        xError.textContent = "";
        xFieldset.classList.remove("invalid-field");
    });
});

yInput.addEventListener("input", function () {
    yError.textContent = "";
    yInput.classList.remove("invalid-field");
});

const rButtons = document.querySelectorAll(".r-button");
const rInput = document.getElementById("r-input");

rButtons.forEach(function (button) {
    button.addEventListener("click", function (event) {
        rButtons.forEach(function (otherButton) {
            otherButton.classList.remove("selected");
        });

        event.currentTarget.classList.add("selected");
        rInput.value = event.currentTarget.value;
        rError.textContent = "";
        rFieldset.classList.remove("invalid-field");
        drawGraph();
    });
});

function refreshResultTimes() {
    results.forEach(function (result, index) {
        resultsBody.rows[index].cells[4].textContent = formatDateTime(result.checkedAt);
    });
}

window.addEventListener("focus", refreshResultTimes);
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
    ctx.arc(centerX, centerY, radius, Math.PI / 2, Math.PI);
    ctx.closePath();
    ctx.fill();

    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.lineTo(centerX, centerY - radius / 2);
    ctx.lineTo(centerX + radius, centerY);
    ctx.closePath();
    ctx.fill();

    ctx.fillRect(centerX, centerY, radius, radius / 2);

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
        const x = centerX + result.x * scale;
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
