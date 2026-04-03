// TrimURL - Dashboard JavaScript

let allUrls = [];

document.addEventListener('DOMContentLoaded', () => {
    loadUrls();

    // Close modal
    const modal = document.getElementById('analyticsModal');
    const closeBtn = document.querySelector('.close-modal');

    closeBtn.addEventListener('click', () => {
        modal.classList.add('hidden');
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.add('hidden');
        }
    });
});

async function loadUrls() {
    try {
        const response = await fetch('/api/urls');
        allUrls = await response.json();
        renderUrlList();
        updateSummaryStats();
    } catch (error) {
        console.error('Failed to load URLs:', error);
    }
}

function renderUrlList() {
    const urlList = document.getElementById('urlList');

    if (allUrls.length === 0) {
        urlList.innerHTML = '<p class="empty-state">No URLs yet. Shorten your first URL!</p>';
        return;
    }

    urlList.innerHTML = allUrls.map(url => `
        <div class="url-item">
            <div class="url-info">
                <div class="short-code">${url.shortCode}</div>
                <div class="original-url" title="${url.originalUrl}">${url.originalUrl}</div>
            </div>
            <div class="url-stats">
                <div class="clicks">${url.totalClicks}</div>
                <div class="clicks-label">clicks</div>
            </div>
            <button class="view-analytics-btn" data-code="${url.shortCode}">View Analytics</button>
        </div>
    `).join('');

    // Add click listeners to buttons
    document.querySelectorAll('.view-analytics-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const shortCode = btn.dataset.code;
            showAnalytics(shortCode);
        });
    });
}

function updateSummaryStats() {
    const totalUrls = allUrls.length;
    const totalClicks = allUrls.reduce((sum, url) => sum + url.totalClicks, 0);

    document.getElementById('totalUrls').textContent = totalUrls;
    document.getElementById('totalClicks').textContent = totalClicks;
}

async function showAnalytics(shortCode) {
    const modal = document.getElementById('analyticsModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalClicks = document.getElementById('modalClicks');
    const browserStats = document.getElementById('browserStats');
    const clickHistory = document.getElementById('clickHistory');

    // Get URL info
    const url = allUrls.find(u => u.shortCode === shortCode);
    if (!url) return;

    modalTitle.textContent = url.shortCode;
    modalClicks.textContent = url.totalClicks;

    // Get clicks data
    try {
        const response = await fetch(`/api/clicks/${shortCode}`);
        const clicks = await response.json();

        if (clicks.length === 0) {
            browserStats.innerHTML = '<p class="empty-state">No clicks yet</p>';
            clickHistory.innerHTML = '<p class="empty-state">No clicks yet</p>';
        } else {
            // Calculate browser stats
            const browserCounts = {};
            clicks.forEach(click => {
                const browser = click.browser || 'Unknown';
                browserCounts[browser] = (browserCounts[browser] || 0) + 1;
            });

            const sortedBrowsers = Object.entries(browserCounts)
                .sort((a, b) => b[1] - a[1]);

            browserStats.innerHTML = sortedBrowsers.map(([browser, count]) => `
                <span class="browser-tag">
                    ${browser}: <span class="count">${count}</span>
                </span>
            `).join('');

            // Show recent clicks (last 10)
            const recentClicks = clicks.slice(-10).reverse();
            clickHistory.innerHTML = recentClicks.map(click => {
                const time = new Date(click.timestamp).toLocaleString();
                return `
                    <div class="click-item">
                        <span class="browser">${click.browser || 'Unknown'}</span>
                        <div>
                            <span class="ip">${click.ipAddress || 'Unknown IP'}</span>
                            <span class="time">${time}</span>
                        </div>
                    </div>
                `;
            }).join('');
        }
    } catch (error) {
        console.error('Failed to load clicks:', error);
    }

    modal.classList.remove('hidden');
}