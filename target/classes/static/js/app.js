// TrimURL - Main Application JavaScript

document.addEventListener('DOMContentLoaded', () => {
    const urlInput = document.getElementById('urlInput');
    const shortenBtn = document.getElementById('shortenBtn');
    const resultDiv = document.getElementById('result');
    const shortUrlInput = document.getElementById('shortUrl');
    const copyBtn = document.getElementById('copyBtn');
    const errorDiv = document.getElementById('error');

    // Shorten URL on button click
    shortenBtn.addEventListener('click', shortenUrl);

    // Shorten URL on Enter key
    urlInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            shortenUrl();
        }
    });

    // Copy to clipboard
    copyBtn.addEventListener('click', copyToClipboard);

    async function shortenUrl() {
        const url = urlInput.value.trim();

        if (!url) {
            showError('Please enter a URL');
            return;
        }

        // Validate URL format
        try {
            new URL(url);
        } catch {
            showError('Please enter a valid URL');
            return;
        }

        // Clear previous states
        hideError();
        resultDiv.classList.add('hidden');

        try {
            const response = await fetch('/api/shorten', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ url: url })
            });

            const data = await response.json();

            if (response.ok) {
                shortUrlInput.value = data.shortUrl;
                resultDiv.classList.remove('hidden');
            } else {
                showError(data.error || 'Failed to shorten URL');
            }
        } catch (error) {
            showError('Network error. Please try again.');
        }
    }

    function copyToClipboard() {
        shortUrlInput.select();
        shortUrlInput.setSelectionRange(0, 99999);

        navigator.clipboard.writeText(shortUrlInput.value).then(() => {
            const originalText = copyBtn.textContent;
            copyBtn.textContent = 'Copied!';
            copyBtn.style.backgroundColor = '#10b981';

            setTimeout(() => {
                copyBtn.textContent = originalText;
                copyBtn.style.backgroundColor = '';
            }, 2000);
        });
    }

    function showError(message) {
        errorDiv.textContent = message;
        errorDiv.classList.remove('hidden');
    }

    function hideError() {
        errorDiv.classList.add('hidden');
    }
});