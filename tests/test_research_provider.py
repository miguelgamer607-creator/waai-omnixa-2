from app.research_web import _DuckDuckGoParser, _is_safe_public_https_url, _resolve_result_url


def test_search_parser_collects_titles_and_snippets():
    html = """
    <div class='result'>
      <a class='result__a' href='https://example.org/page'>Example result</a>
      <a class='result__snippet'>A short extract from the page.</a>
    </div>
    """
    parser = _DuckDuckGoParser()
    parser.feed(html)
    assert parser.results == [{"title": "Example result", "url": "https://example.org/page", "snippet": "A short extract from the page."}]


def test_research_url_filter_respects_domains_and_rejects_http_or_local_targets():
    assert _is_safe_public_https_url("https://docs.example.org/page", ["example.org"])
    assert not _is_safe_public_https_url("https://example.net/page", ["example.org"])
    assert not _is_safe_public_https_url("http://example.org/page", ["example.org"])
    assert not _is_safe_public_https_url("https://localhost/admin", [])
    assert not _is_safe_public_https_url("https://127.0.0.1/admin", [])


def test_search_redirect_is_unwrapped_without_fetching_target():
    url = "https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.org%2Fpage"
    assert _resolve_result_url(url) == "https://example.org/page"
