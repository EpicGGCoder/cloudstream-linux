/**
 * HTTP helper — TypeScript port of CS3's `app` (NiceHttp Requests wrapper).
 *
 * CS3 uses `app.get(url, ...)` returning a `NiceResponse` with `.text`,
 * `.document` (Ksoup-parsed HTML), `.parsed<T>()`, `.url`, `.headers`.
 * We replicate the same surface so plugins port 1:1.
 *
 * See ARCHITECTURE.md §10 for full reference.
 */

import * as cheerio from "cheerio";

export const USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

export interface RequestOptions {
  headers?: Record<string, string>;
  referer?: string;
  params?: Record<string, string | number | boolean | undefined>;
  data?: Record<string, string>; // form-encoded body
  json?: unknown; // JSON body
  requestBody?: string; // raw body
  timeout?: number; // ms
  allowRedirects?: boolean;
  verify?: boolean; // false to skip SSL (n/a in fetch; ignored)
  cacheTime?: number; // seconds — HTTP cache control override (0 = no cache)
  cacheUnit?: "second" | "minute" | "hour";
}

export class NiceResponse {
  constructor(
    private readonly resp: Response,
    public readonly url: string,
    public readonly status: number,
  ) {}

  private _text?: string;
  async text(): Promise<string> {
    if (this._text === undefined) this._text = await this.resp.text();
    return this._text;
  }

  private _$?: cheerio.CheerioAPI;
  async document(): Promise<cheerio.CheerioAPI> {
    if (this._$ === undefined) this._$ = cheerio.load(await this.text());
    return this._$;
  }

  async parsed<T>(): Promise<T> {
    return JSON.parse(await this.text()) as T;
  }

  async parsedSafe<T>(): Promise<T | null> {
    try {
      return JSON.parse(await this.text()) as T;
    } catch {
      return null;
    }
  }

  get headers(): Record<string, string> {
    const h: Record<string, string> = {};
    this.resp.headers.forEach((v, k) => (h[k] = v));
    return h;
  }

  get ok(): boolean {
    return this.resp.ok;
  }
}

class Requests {
  defaultHeaders: Record<string, string> = {
    "user-agent": USER_AGENT,
  };

  private buildUrl(
    url: string,
    params?: Record<string, string | number | boolean | undefined>,
  ): string {
    if (!params) return url;
    const u = new URL(url);
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined) u.searchParams.set(k, String(v));
    }
    return u.toString();
  }

  async get(url: string, opts: RequestOptions = {}): Promise<NiceResponse> {
    return this.request("GET", url, opts);
  }

  async post(url: string, opts: RequestOptions = {}): Promise<NiceResponse> {
    return this.request("POST", url, opts);
  }

  async head(url: string, opts: RequestOptions = {}): Promise<NiceResponse> {
    return this.request("HEAD", url, opts);
  }

  private async request(
    method: string,
    url: string,
    opts: RequestOptions,
  ): Promise<NiceResponse> {
    const finalUrl = this.buildUrl(url, opts.params);
    const headers: Record<string, string> = {
      ...this.defaultHeaders,
      ...opts.headers,
    };
    if (opts.referer) headers["referer"] = opts.referer;

    let body: BodyInit | undefined;
    if (opts.json !== undefined) {
      body = JSON.stringify(opts.json);
      headers["content-type"] = "application/json";
    } else if (opts.data !== undefined) {
      body = new URLSearchParams(opts.data).toString();
      headers["content-type"] = "application/x-www-form-urlencoded";
    } else if (opts.requestBody !== undefined) {
      body = opts.requestBody;
    }

    const controller = new AbortController();
    const timeoutMs = opts.timeout ?? 30_000;
    const t = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const resp = await fetch(finalUrl, {
        method,
        headers,
        body,
        redirect: opts.allowRedirects === false ? "manual" : "follow",
        signal: controller.signal,
      });

      // Followed redirects return the final URL via Response.url
      return new NiceResponse(resp, resp.url || finalUrl, resp.status);
    } finally {
      clearTimeout(t);
    }
  }
}

/** CS3-equivalent `app` global HTTP client. */
export const app = new Requests();
